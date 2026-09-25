package vn.edu.fpt.cares.controller;

import vn.edu.fpt.cares.enums.ChatSenderType;
import vn.edu.fpt.cares.enums.ChatSessionStatus;
import vn.edu.fpt.cares.enums.Role;
import vn.edu.fpt.cares.exception.BadRequestException;
import vn.edu.fpt.cares.model.*;
import vn.edu.fpt.cares.repository.AccountRepository;
import vn.edu.fpt.cares.repository.ProfileRepository;
import vn.edu.fpt.cares.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {
    @Mock ChatService chatService;
    @Mock AccountRepository accountRepo;
    @Mock ProfileRepository profileRepo;
    private ChatController controller;
    private UUID accountId;
    private UUID profileId;
    private Account account;
    private Profile profile;

    @BeforeEach
    void setUp() {
        controller = new ChatController(chatService, accountRepo, profileRepo);
        accountId = UUID.randomUUID();
        profileId = UUID.randomUUID();
        account = Account.builder().accountId(accountId).username("nguyenanhduc").role(Role.CUSTOMER).build();
        profile = Profile.builder().profileId(profileId).account(account).fullName("Nguyễn Anh Đức")
                .patientCode("BN-001").build();
    }

    @Test
    void customerStartsSessionUsingMapPrincipal() {
        Authentication auth = auth(Map.of("username", "nguyenanhduc"), "ROLE_CUSTOMER");
        ChatSession session = session(profile, ChatSessionStatus.BOT_HANDLING);
        when(accountRepo.findFirstByUsername("nguyenanhduc")).thenReturn(Optional.of(account));
        when(profileRepo.findFirstByAccount_AccountId(accountId)).thenReturn(Optional.of(profile));
        when(chatService.startOrGetActiveSession(profileId)).thenReturn(session);
        Map<?, ?> body = (Map<?, ?>) controller.startOrGetSession(auth).getBody();
        assertEquals(session.getSessionId(), body.get("sessionId"));
        assertEquals(profileId, body.get("profileId"));
    }

    @Test
    void guestSessionValidatesAndTrimsIdentity() {
        assertEquals(400, controller.startOrGetGuestSession(Map.of("phone", "0987654321")).getStatusCode().value());
        assertEquals(400, controller.startOrGetGuestSession(Map.of("fullName", "Anh Đức")).getStatusCode().value());
        assertEquals(400, controller.startOrGetGuestSession(Map.of("fullName", " ", "phone", "0987")).getStatusCode().value());
        assertEquals(400, controller.startOrGetGuestSession(Map.of("fullName", "Anh Đức", "phone", " ")).getStatusCode().value());

        Profile guest = Profile.builder().profileId(UUID.randomUUID()).fullName("Khách demo").build();
        ChatSession session = session(guest, ChatSessionStatus.WAITING_FOR_AGENT);
        when(chatService.startOrGetGuestSession("Khách demo", "0987654321")).thenReturn(session);
        Map<?, ?> body = (Map<?, ?>) controller.startOrGetGuestSession(
                Map.of("fullName", "  Khách demo ", "phone", " 0987654321 ")).getBody();
        assertEquals(guest.getProfileId(), body.get("guestProfileId"));
    }

    @Test
    void activeAndClosedSessionListsMapFallbackFieldsAndTimestamps() {
        Profile noCode = mock(Profile.class);
        when(noCode.getFullName()).thenReturn("Khách A");
        when(noCode.getPatientCode()).thenReturn(null);
        ChatSession first = session(noCode, ChatSessionStatus.WAITING_FOR_AGENT);
        first.setCreatedAt(LocalDateTime.of(2026, 9, 5, 9, 0));
        ChatSession second = session(profile, ChatSessionStatus.CLOSED);
        second.setAssignedReceptionistId(UUID.randomUUID());
        second.setUpdatedAt(LocalDateTime.of(2026, 9, 5, 10, 0));
        when(chatService.getActiveSessionsForReceptionist()).thenReturn(List.of(first, second));
        when(chatService.getClosedSessionsForReceptionist()).thenReturn(List.of(second, first));
        List<?> active = (List<?>) controller.getActiveSessionsForReceptionist().getBody();
        List<?> closed = (List<?>) controller.getClosedSessionsForReceptionist().getBody();
        assertEquals(2, active.size());
        assertEquals(2, closed.size());
        assertEquals("Chưa có", ((Map<?, ?>) active.get(0)).get("patientCode"));
        assertEquals("", ((Map<?, ?>) active.get(0)).get("assignedReceptionistId"));
    }

    @Test
    void authenticatedMessagesAllowReceptionistWithoutOwnershipLookup() {
        UUID sessionId = UUID.randomUUID();
        Authentication receptionist = auth("receptionist", "ROLE_RECEPTIONIST");
        ChatMessage message = message(sessionId);
        when(chatService.getMessages(sessionId)).thenReturn(List.of(message));
        List<?> body = (List<?>) controller.getMessages(sessionId, receptionist).getBody();
        assertEquals(1, body.size());
        verify(accountRepo, never()).findFirstByUsername(anyString());

        ChatSession session = session(profile, ChatSessionStatus.IN_PROGRESS);
        when(chatService.getSession(sessionId)).thenReturn(session);
        Map<?, ?> status = (Map<?, ?>) controller.getSessionStatus(sessionId,
                auth("manager", "ROLE_CLINIC_MANAGER")).getBody();
        assertEquals(ChatSessionStatus.IN_PROGRESS, status.get("status"));
    }

    @Test
    void authenticatedCustomerMustOwnSession() {
        UUID sessionId = UUID.randomUUID();
        Authentication customerAuth = auth("nguyenanhduc", "ROLE_CUSTOMER");
        when(accountRepo.findFirstByUsername("nguyenanhduc")).thenReturn(Optional.of(account));
        when(profileRepo.findFirstByAccount_AccountId(accountId)).thenReturn(Optional.of(profile));
        when(chatService.getSession(sessionId)).thenReturn(session(profile, ChatSessionStatus.BOT_HANDLING));
        when(chatService.getMessages(sessionId)).thenReturn(List.of(message(sessionId)));
        assertEquals(1, ((List<?>) controller.getMessages(sessionId, customerAuth).getBody()).size());

        Profile other = Profile.builder().profileId(UUID.randomUUID()).build();
        when(chatService.getSession(sessionId)).thenReturn(session(other, ChatSessionStatus.BOT_HANDLING));
        assertThrows(BadRequestException.class, () -> controller.getSessionStatus(sessionId, customerAuth));
        when(chatService.getSession(sessionId)).thenReturn(ChatSession.builder().sessionId(sessionId).customer(null).build());
        assertThrows(BadRequestException.class, () -> controller.getMessages(sessionId, customerAuth));
    }

    @Test
    void guestMessagesAndStatusRequireMatchingAccountlessProfile() {
        UUID sessionId = UUID.randomUUID();
        Profile guest = Profile.builder().profileId(UUID.randomUUID()).build();
        ChatSession session = session(guest, ChatSessionStatus.BOT_HANDLING);
        when(chatService.getSession(sessionId)).thenReturn(session);
        when(chatService.getMessages(sessionId)).thenReturn(List.of(message(sessionId)));
        assertEquals(1, ((List<?>) controller.getGuestMessages(sessionId, guest.getProfileId()).getBody()).size());
        assertEquals(ChatSessionStatus.BOT_HANDLING,
                ((Map<?, ?>) controller.getGuestSessionStatus(sessionId, guest.getProfileId()).getBody()).get("status"));

        assertThrows(BadRequestException.class,
                () -> controller.getGuestMessages(sessionId, UUID.randomUUID()));
        guest.setAccount(account);
        assertThrows(BadRequestException.class,
                () -> controller.getGuestSessionStatus(sessionId, guest.getProfileId()));
        session.setCustomer(null);
        assertThrows(BadRequestException.class,
                () -> controller.getGuestSessionStatus(sessionId, guest.getProfileId()));
    }

    @Test
    void customerMessageValidatesContentAndUsesAuthenticatedProfile() {
        UUID sessionId = UUID.randomUUID();
        Authentication auth = auth("nguyenanhduc", "ROLE_CUSTOMER");
        assertEquals(400, controller.sendCustomerMessage(sessionId, Map.of(), auth).getStatusCode().value());
        assertEquals(400, controller.sendCustomerMessage(sessionId, Map.of("content", " "), auth).getStatusCode().value());
        assertEquals(400, controller.sendCustomerMessage(sessionId,
                Map.of("content", "x".repeat(201)), auth).getStatusCode().value());
        when(accountRepo.findFirstByUsername("nguyenanhduc")).thenReturn(Optional.of(account));
        when(profileRepo.findFirstByAccount_AccountId(accountId)).thenReturn(Optional.of(profile));
        assertEquals(200, controller.sendCustomerMessage(sessionId,
                Map.of("content", "  Tôi cần hỗ trợ "), auth).getStatusCode().value());
        verify(chatService).processCustomerMessage(sessionId, profileId, "Tôi cần hỗ trợ");
    }

    @Test
    void guestMessageValidatesIdentityAndSendsTrimmedContent() {
        UUID sessionId = UUID.randomUUID();
        assertEquals(400, controller.sendGuestMessage(sessionId, Map.of()).getStatusCode().value());
        assertEquals(400, controller.sendGuestMessage(sessionId,
                Map.of("content", " ", "guestProfileId", UUID.randomUUID().toString())).getStatusCode().value());
        assertEquals(400, controller.sendGuestMessage(sessionId,
                Map.of("content", "x".repeat(201), "guestProfileId", UUID.randomUUID().toString())).getStatusCode().value());
        assertEquals(400, controller.sendGuestMessage(sessionId,
                Map.of("content", "hello")).getStatusCode().value());

        Profile guest = Profile.builder().profileId(UUID.randomUUID()).build();
        when(chatService.getSession(sessionId)).thenReturn(session(guest, ChatSessionStatus.BOT_HANDLING));
        assertEquals(200, controller.sendGuestMessage(sessionId, Map.of(
                "content", "  Xin chào ", "guestProfileId", guest.getProfileId().toString())).getStatusCode().value());
        verify(chatService).processCustomerMessage(sessionId, guest.getProfileId(), "Xin chào");
    }

    @Test
    void receptionistMessageValidatesAndUsesAccountIdThenCanClose() {
        UUID sessionId = UUID.randomUUID();
        Authentication auth = auth("receptionist", "ROLE_RECEPTIONIST");
        assertEquals(400, controller.sendReceptionistMessage(sessionId, Map.of(), auth).getStatusCode().value());
        assertEquals(400, controller.sendReceptionistMessage(sessionId, Map.of("content", " "), auth).getStatusCode().value());
        assertEquals(400, controller.sendReceptionistMessage(sessionId,
                Map.of("content", "x".repeat(201)), auth).getStatusCode().value());
        Account staff = Account.builder().accountId(UUID.randomUUID()).username("receptionist").role(Role.STAFF).build();
        when(accountRepo.findFirstByUsername("receptionist")).thenReturn(Optional.of(staff));
        assertEquals(200, controller.sendReceptionistMessage(sessionId,
                Map.of("content", "  Đã tiếp nhận "), auth).getStatusCode().value());
        verify(chatService).processReceptionistMessage(sessionId, staff.getAccountId(), "Đã tiếp nhận");
        assertEquals(200, controller.closeSession(sessionId).getStatusCode().value());
        verify(chatService).closeSession(sessionId);
    }

    private Authentication auth(Object principal, String authority) {
        return new UsernamePasswordAuthenticationToken(principal, "x",
                List.of(new SimpleGrantedAuthority(authority)));
    }

    private ChatSession session(Profile customer, ChatSessionStatus status) {
        ChatSession session = ChatSession.builder().sessionId(UUID.randomUUID()).customer(customer).status(status).build();
        session.setCreatedAt(LocalDateTime.of(2026, 9, 5, 8, 0));
        return session;
    }

    private ChatMessage message(UUID sessionId) {
        ChatMessage message = ChatMessage.builder().messageId(UUID.randomUUID())
                .session(ChatSession.builder().sessionId(sessionId).build())
                .senderType(ChatSenderType.CUSTOMER).content("Xin chào").build();
        message.setCreatedAt(LocalDateTime.of(2026, 9, 5, 8, 5));
        return message;
    }
}
