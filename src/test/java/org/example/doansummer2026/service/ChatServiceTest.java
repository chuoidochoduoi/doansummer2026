package org.example.doansummer2026.service;

import org.example.doansummer2026.enums.ChatSenderType;
import org.example.doansummer2026.enums.ChatSessionStatus;
import org.example.doansummer2026.model.ChatMessage;
import org.example.doansummer2026.model.ChatSession;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.repository.ChatMessageRepository;
import org.example.doansummer2026.repository.ChatSessionRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatSessionRepository sessionRepo;

    @Mock
    private ChatMessageRepository messageRepo;

    @Mock
    private ProfileRepository profileRepo;

    @Mock
    private RuleBasedBotService botService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatService chatService;


    // =========================================================
    // HELPERS
    // =========================================================

    private Profile profile(UUID id, String name, String phone) {
        return Profile.builder()
                .profileId(id)
                .fullName(name)
                .phone(phone)
                .build();
    }

    private ChatSession session(
            UUID id,
            Profile customer,
            ChatSessionStatus status
    ) {
        return ChatSession.builder()
                .sessionId(id)
                .customer(customer)
                .status(status)
                .build();
    }


    // =========================================================
    // START OR GET ACTIVE SESSION
    // =========================================================

    @Test
    void startOrGetActiveSession_ShouldReturnExistingActiveSession() {

        UUID customerId = UUID.randomUUID();

        Profile customer =
                profile(customerId, "Customer", "0901234567");

        ChatSession closed =
                session(
                        UUID.randomUUID(),
                        customer,
                        ChatSessionStatus.CLOSED
                );

        ChatSession active =
                session(
                        UUID.randomUUID(),
                        customer,
                        ChatSessionStatus.BOT_HANDLING
                );

        when(sessionRepo.findByCustomer_ProfileId(customerId))
                .thenReturn(List.of(closed, active));

        ChatSession result =
                chatService.startOrGetActiveSession(customerId);

        assertSame(active, result);

        verify(profileRepo, never())
                .findById(any());

        verify(sessionRepo, never())
                .save(any());
    }


    @Test
    void startOrGetActiveSession_ShouldCreateNewSession_WhenNoActiveSession() {

        UUID customerId = UUID.randomUUID();

        Profile customer =
                profile(customerId, "Customer", "0901234567");

        ChatSession closed =
                session(
                        UUID.randomUUID(),
                        customer,
                        ChatSessionStatus.CLOSED
                );

        when(sessionRepo.findByCustomer_ProfileId(customerId))
                .thenReturn(List.of(closed));

        when(profileRepo.findById(customerId))
                .thenReturn(Optional.of(customer));

        when(sessionRepo.save(any(ChatSession.class)))
                .thenAnswer(invocation -> {
                    ChatSession s = invocation.getArgument(0);
                    s.setSessionId(UUID.randomUUID());
                    return s;
                });

        ChatSession result =
                chatService.startOrGetActiveSession(customerId);

        assertNotNull(result);

        assertSame(
                customer,
                result.getCustomer()
        );

        assertEquals(
                ChatSessionStatus.BOT_HANDLING,
                result.getStatus()
        );

        verify(sessionRepo)
                .save(any(ChatSession.class));
    }


    @Test
    void startOrGetActiveSession_ShouldThrow_WhenProfileMissing() {

        UUID customerId = UUID.randomUUID();

        when(sessionRepo.findByCustomer_ProfileId(customerId))
                .thenReturn(List.of());

        when(profileRepo.findById(customerId))
                .thenReturn(Optional.empty());

        RuntimeException ex =
                assertThrows(
                        RuntimeException.class,
                        () -> chatService.startOrGetActiveSession(customerId)
                );

        assertEquals(
                "Profile not found",
                ex.getMessage()
        );

        verify(sessionRepo, never())
                .save(any());
    }


    // =========================================================
    // GUEST SESSION
    // =========================================================

    @Test
    void startOrGetGuestSession_ShouldReuseExistingProfile() {

        UUID profileId = UUID.randomUUID();

        Profile guest =
                profile(
                        profileId,
                        "Guest",
                        "0909999999"
                );

        ChatSession active =
                session(
                        UUID.randomUUID(),
                        guest,
                        ChatSessionStatus.WAITING_FOR_AGENT
                );

        when(profileRepo.findFirstByPhone("0909999999"))
                .thenReturn(Optional.of(guest));

        when(sessionRepo.findByCustomer_ProfileId(profileId))
                .thenReturn(List.of(active));

        ChatSession result =
                chatService.startOrGetGuestSession(
                        "Guest",
                        "0909999999"
                );

        assertSame(active, result);

        verify(profileRepo, never())
                .save(any(Profile.class));

        verify(sessionRepo, never())
                .save(any(ChatSession.class));
    }


    @Test
    void startOrGetGuestSession_ShouldCreateProfileAndSession_WhenGuestIsNew() {

        UUID profileId = UUID.randomUUID();

        when(profileRepo.findFirstByPhone("0911111111"))
                .thenReturn(Optional.empty());

        when(profileRepo.save(any(Profile.class)))
                .thenAnswer(invocation -> {
                    Profile p = invocation.getArgument(0);
                    p.setProfileId(profileId);
                    return p;
                });

        when(sessionRepo.findByCustomer_ProfileId(profileId))
                .thenReturn(List.of());

        when(sessionRepo.save(any(ChatSession.class)))
                .thenAnswer(invocation -> {
                    ChatSession s = invocation.getArgument(0);
                    s.setSessionId(UUID.randomUUID());
                    return s;
                });

        ChatSession result =
                chatService.startOrGetGuestSession(
                        "Guest New",
                        "0911111111"
                );

        assertNotNull(result);

        assertEquals(
                ChatSessionStatus.BOT_HANDLING,
                result.getStatus()
        );

        verify(profileRepo)
                .save(argThat(p ->
                        "Guest New".equals(p.getFullName())
                                && "0911111111".equals(p.getPhone())
                ));

        verify(sessionRepo)
                .save(any(ChatSession.class));
    }


    @Test
    void startOrGetGuestSession_ShouldCreateNewSession_WhenExistingSessionsAreClosed() {

        UUID profileId = UUID.randomUUID();

        Profile guest =
                profile(
                        profileId,
                        "Guest",
                        "0922222222"
                );

        ChatSession closed =
                session(
                        UUID.randomUUID(),
                        guest,
                        ChatSessionStatus.CLOSED
                );

        when(profileRepo.findFirstByPhone("0922222222"))
                .thenReturn(Optional.of(guest));

        when(sessionRepo.findByCustomer_ProfileId(profileId))
                .thenReturn(List.of(closed));

        when(sessionRepo.save(any(ChatSession.class)))
                .thenAnswer(invocation -> {
                    ChatSession s = invocation.getArgument(0);
                    s.setSessionId(UUID.randomUUID());
                    return s;
                });

        ChatSession result =
                chatService.startOrGetGuestSession(
                        "Guest",
                        "0922222222"
                );

        assertEquals(
                ChatSessionStatus.BOT_HANDLING,
                result.getStatus()
        );
    }


    // =========================================================
    // CUSTOMER MESSAGE - SESSION MISSING
    // =========================================================

    @Test
    void processCustomerMessage_ShouldThrow_WhenSessionMissing() {

        UUID sessionId = UUID.randomUUID();

        when(sessionRepo.findById(sessionId))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> chatService.processCustomerMessage(
                        sessionId,
                        UUID.randomUUID(),
                        "Hello"
                )
        );

        verifyNoInteractions(messageRepo);
    }


    // =========================================================
    // CUSTOMER MESSAGE - WAITING FOR AGENT
    // =========================================================

    @Test
    void processCustomerMessage_ShouldBroadcastToReceptionist_WhenWaitingForAgent() {

        UUID sessionId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        ChatSession session =
                session(
                        sessionId,
                        profile(customerId, "Customer", "0901"),
                        ChatSessionStatus.WAITING_FOR_AGENT
                );

        when(sessionRepo.findById(sessionId))
                .thenReturn(Optional.of(session));

        chatService.processCustomerMessage(
                sessionId,
                customerId,
                "Can ho tro"
        );

        verify(messageRepo)
                .save(argThat(msg ->
                        msg.getSession() == session
                                && msg.getSenderType()
                                == ChatSenderType.CUSTOMER
                                && customerId.equals(msg.getSenderId())
                                && "Can ho tro".equals(msg.getContent())
                ));

        verify(messagingTemplate)
                .convertAndSend(
                        "/topic/receptionist-chat",
                        "NEW_MESSAGE"
                );

        verify(messagingTemplate)
                .convertAndSend(
                        "/topic/chat-" + sessionId,
                        "NEW_MESSAGE"
                );

        verifyNoInteractions(botService);
    }


    // =========================================================
    // CUSTOMER MESSAGE - IN PROGRESS
    // =========================================================

    @Test
    void processCustomerMessage_ShouldBroadcastToReceptionist_WhenInProgress() {

        UUID sessionId = UUID.randomUUID();

        ChatSession session =
                session(
                        sessionId,
                        profile(UUID.randomUUID(), "Customer", "0901"),
                        ChatSessionStatus.IN_PROGRESS
                );

        when(sessionRepo.findById(sessionId))
                .thenReturn(Optional.of(session));

        chatService.processCustomerMessage(
                sessionId,
                UUID.randomUUID(),
                "Message"
        );

        verify(messagingTemplate)
                .convertAndSend(
                        "/topic/receptionist-chat",
                        "NEW_MESSAGE"
                );

        verifyNoInteractions(botService);
    }


    // =========================================================
    // CUSTOMER MESSAGE - CLOSED
    // =========================================================

    @Test
    void processCustomerMessage_ShouldNotCallBotOrReceptionistTopic_WhenClosed() {

        UUID sessionId = UUID.randomUUID();

        ChatSession session =
                session(
                        sessionId,
                        profile(UUID.randomUUID(), "Customer", "0901"),
                        ChatSessionStatus.CLOSED
                );

        when(sessionRepo.findById(sessionId))
                .thenReturn(Optional.of(session));

        chatService.processCustomerMessage(
                sessionId,
                UUID.randomUUID(),
                "Message"
        );

        verify(messagingTemplate, never())
                .convertAndSend(
                        "/topic/receptionist-chat",
                        "NEW_MESSAGE"
                );

        verify(messagingTemplate)
                .convertAndSend(
                        "/topic/chat-" + sessionId,
                        "NEW_MESSAGE"
                );

        verifyNoInteractions(botService);
    }


    // =========================================================
    // CUSTOMER MESSAGE - BOT NORMAL RESPONSE
    // =========================================================

    @Test
    void processCustomerMessage_ShouldSaveBotReply_WhenBotHandlesNormally() {

        UUID sessionId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        ChatSession session =
                session(
                        sessionId,
                        profile(customerId, "Customer", "0901"),
                        ChatSessionStatus.BOT_HANDLING
                );

        when(sessionRepo.findById(sessionId))
                .thenReturn(Optional.of(session));

        when(botService.getBotResponse("Xin chao"))
                .thenReturn("Chao ban");

        chatService.processCustomerMessage(
                sessionId,
                customerId,
                "Xin chao"
        );

        ArgumentCaptor<ChatMessage> captor =
                ArgumentCaptor.forClass(ChatMessage.class);

        verify(messageRepo, times(2))
                .save(captor.capture());

        List<ChatMessage> saved =
                captor.getAllValues();

        ChatMessage customerMsg =
                saved.get(0);

        ChatMessage botMsg =
                saved.get(1);

        assertEquals(
                ChatSenderType.CUSTOMER,
                customerMsg.getSenderType()
        );

        assertEquals(
                ChatSenderType.BOT,
                botMsg.getSenderType()
        );

        assertEquals(
                "Chao ban",
                botMsg.getContent()
        );

        verify(messagingTemplate, times(2))
                .convertAndSend(
                        "/topic/chat-" + sessionId,
                        "NEW_MESSAGE"
                );

        verify(sessionRepo, never())
                .save(session);
    }


    // =========================================================
    // CUSTOMER MESSAGE - HANDOVER
    // =========================================================

    @Test
    void processCustomerMessage_ShouldHandoverToReceptionist() {

        UUID sessionId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        ChatSession session =
                session(
                        sessionId,
                        profile(customerId, "Customer", "0901"),
                        ChatSessionStatus.BOT_HANDLING
                );

        when(sessionRepo.findById(sessionId))
                .thenReturn(Optional.of(session));

        when(botService.getBotResponse("Can le tan"))
                .thenReturn("[HANDOVER]");

        chatService.processCustomerMessage(
                sessionId,
                customerId,
                "Can le tan"
        );

        assertEquals(
                ChatSessionStatus.WAITING_FOR_AGENT,
                session.getStatus()
        );

        verify(sessionRepo)
                .save(session);

        verify(messageRepo, times(2))
                .save(any(ChatMessage.class));

        verify(messageRepo)
                .save(argThat(msg ->
                        msg.getSenderType() == ChatSenderType.BOT
                                && msg.getContent() != null
                                && msg.getContent()
                                .contains("chuyển đến Lễ tân")
                ));

        verify(messagingTemplate)
                .convertAndSend(
                        "/topic/receptionist-chat",
                        "NEW_CHAT_REQUEST"
                );

        verify(messagingTemplate, times(2))
                .convertAndSend(
                        "/topic/chat-" + sessionId,
                        "NEW_MESSAGE"
                );
    }


    // =========================================================
    // RECEPTIONIST MESSAGE - SESSION MISSING
    // =========================================================

    @Test
    void processReceptionistMessage_ShouldThrow_WhenSessionMissing() {

        UUID sessionId = UUID.randomUUID();

        when(sessionRepo.findById(sessionId))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> chatService.processReceptionistMessage(
                        sessionId,
                        UUID.randomUUID(),
                        "Hello"
                )
        );
    }


    // =========================================================
    // RECEPTIONIST MESSAGE - ACCEPT WAITING CHAT
    // =========================================================

    @Test
    void processReceptionistMessage_ShouldAcceptWaitingChat() {

        UUID sessionId = UUID.randomUUID();
        UUID receptionistId = UUID.randomUUID();

        ChatSession session =
                session(
                        sessionId,
                        profile(UUID.randomUUID(), "Customer", "0901"),
                        ChatSessionStatus.WAITING_FOR_AGENT
                );

        when(sessionRepo.findById(sessionId))
                .thenReturn(Optional.of(session));

        chatService.processReceptionistMessage(
                sessionId,
                receptionistId,
                "Toi se ho tro"
        );

        assertEquals(
                ChatSessionStatus.IN_PROGRESS,
                session.getStatus()
        );

        assertEquals(
                receptionistId,
                session.getAssignedReceptionistId()
        );

        verify(sessionRepo)
                .save(session);

        verify(messagingTemplate)
                .convertAndSend(
                        "/topic/receptionist-chat",
                        "CHAT_ACCEPTED"
                );

        verify(messageRepo)
                .save(argThat(msg ->
                        msg.getSenderType()
                                == ChatSenderType.RECEPTIONIST
                                && receptionistId.equals(
                                msg.getSenderId()
                        )
                                && "Toi se ho tro".equals(
                                msg.getContent()
                        )
                ));

        verify(messagingTemplate)
                .convertAndSend(
                        "/topic/chat-" + sessionId,
                        "NEW_MESSAGE"
                );
    }


    // =========================================================
    // RECEPTIONIST MESSAGE - ALREADY IN PROGRESS
    // =========================================================

    @Test
    void processReceptionistMessage_ShouldNotReassign_WhenAlreadyInProgress() {

        UUID sessionId = UUID.randomUUID();
        UUID oldReceptionistId = UUID.randomUUID();
        UUID newReceptionistId = UUID.randomUUID();

        ChatSession session =
                session(
                        sessionId,
                        profile(UUID.randomUUID(), "Customer", "0901"),
                        ChatSessionStatus.IN_PROGRESS
                );

        session.setAssignedReceptionistId(oldReceptionistId);

        when(sessionRepo.findById(sessionId))
                .thenReturn(Optional.of(session));

        chatService.processReceptionistMessage(
                sessionId,
                newReceptionistId,
                "Message"
        );

        assertEquals(
                oldReceptionistId,
                session.getAssignedReceptionistId()
        );

        verify(sessionRepo, never())
                .save(session);

        verify(messagingTemplate, never())
                .convertAndSend(
                        "/topic/receptionist-chat",
                        "CHAT_ACCEPTED"
                );

        verify(messageRepo)
                .save(any(ChatMessage.class));
    }


    // =========================================================
    // CLOSE SESSION
    // =========================================================

    @Test
    void closeSession_ShouldThrow_WhenMissing() {

        UUID sessionId = UUID.randomUUID();

        when(sessionRepo.findById(sessionId))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> chatService.closeSession(sessionId)
        );
    }


    @Test
    void closeSession_ShouldCloseAndBroadcast() {

        UUID sessionId = UUID.randomUUID();

        ChatSession session =
                session(
                        sessionId,
                        profile(UUID.randomUUID(), "Customer", "0901"),
                        ChatSessionStatus.IN_PROGRESS
                );

        when(sessionRepo.findById(sessionId))
                .thenReturn(Optional.of(session));

        chatService.closeSession(sessionId);

        assertEquals(
                ChatSessionStatus.CLOSED,
                session.getStatus()
        );

        verify(sessionRepo)
                .save(session);

        verify(messagingTemplate)
                .convertAndSend(
                        "/topic/chat-" + sessionId,
                        "SESSION_CLOSED"
                );

        verify(messagingTemplate)
                .convertAndSend(
                        "/topic/receptionist-chat",
                        "SESSION_CLOSED"
                );
    }


    // =========================================================
    // ACTIVE SESSIONS FOR RECEPTIONIST
    // =========================================================

    @Test
    void getActiveSessionsForReceptionist_ShouldCombineWaitingAndInProgress() {

        ChatSession waiting =
                session(
                        UUID.randomUUID(),
                        null,
                        ChatSessionStatus.WAITING_FOR_AGENT
                );

        ChatSession inProgress =
                session(
                        UUID.randomUUID(),
                        null,
                        ChatSessionStatus.IN_PROGRESS
                );

        List<ChatSession> waitingList =
                new ArrayList<>();

        waitingList.add(waiting);

        when(
                sessionRepo.findByStatus(
                        ChatSessionStatus.WAITING_FOR_AGENT
                )
        ).thenReturn(waitingList);

        when(
                sessionRepo.findByStatus(
                        ChatSessionStatus.IN_PROGRESS
                )
        ).thenReturn(List.of(inProgress));

        List<ChatSession> result =
                chatService.getActiveSessionsForReceptionist();

        assertEquals(2, result.size());

        assertTrue(result.contains(waiting));
        assertTrue(result.contains(inProgress));
    }


    // =========================================================
    // CLOSED SESSIONS
    // =========================================================

    @Test
    void getClosedSessionsForReceptionist_ShouldDelegate() {

        ChatSession closed =
                session(
                        UUID.randomUUID(),
                        null,
                        ChatSessionStatus.CLOSED
                );

        when(
                sessionRepo.findByStatus(
                        ChatSessionStatus.CLOSED
                )
        ).thenReturn(List.of(closed));

        List<ChatSession> result =
                chatService.getClosedSessionsForReceptionist();

        assertEquals(1, result.size());
        assertSame(closed, result.get(0));
    }


    // =========================================================
    // GET MESSAGES
    // =========================================================

    @Test
    void getMessages_ShouldDelegateToRepository() {

        UUID sessionId = UUID.randomUUID();

        ChatMessage message =
                ChatMessage.builder()
                        .content("Hello")
                        .build();

        when(
                messageRepo
                        .findBySession_SessionIdOrderByCreatedAtAsc(
                                sessionId
                        )
        ).thenReturn(List.of(message));

        List<ChatMessage> result =
                chatService.getMessages(sessionId);

        assertEquals(1, result.size());
        assertSame(message, result.get(0));
    }


    // =========================================================
    // GET SESSION
    // =========================================================

    @Test
    void getSession_ShouldReturn_WhenFound() {

        UUID sessionId = UUID.randomUUID();

        ChatSession session =
                session(
                        sessionId,
                        null,
                        ChatSessionStatus.BOT_HANDLING
                );

        when(sessionRepo.findById(sessionId))
                .thenReturn(Optional.of(session));

        assertSame(
                session,
                chatService.getSession(sessionId)
        );
    }


    @Test
    void getSession_ShouldThrow_WhenMissing() {

        UUID sessionId = UUID.randomUUID();

        when(sessionRepo.findById(sessionId))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> chatService.getSession(sessionId)
        );
    }
}