package vn.edu.fpt.cares.config;

import io.jsonwebtoken.Claims;
import vn.edu.fpt.cares.model.Account;
import vn.edu.fpt.cares.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MembershipWebSocketSecurityTest {
    private final JwtService jwt = mock(JwtService.class);
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final WebSocketConfig config = new WebSocketConfig(jwt, accounts);

    private Message<byte[]> message(StompHeaderAccessor accessor) {
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void connectsUsingVerifiedAccountIdAndSubscribesToOwnQueue() {
        UUID owner = UUID.randomUUID();
        Claims claims = mock(Claims.class);
        when(jwt.parseClaims("valid")).thenReturn(claims);
        when(claims.get("type", String.class)).thenReturn("access");
        when(claims.getSubject()).thenReturn("customer");
        when(claims.get("uid", String.class)).thenReturn(owner.toString());
        when(accounts.findFirstByUsername("customer")).thenReturn(Optional.of(
                Account.builder().accountId(owner).isActive(true).build()));
        var connect = StompHeaderAccessor.create(StompCommand.CONNECT);
        connect.setNativeHeader("Authorization", "Bearer valid");
        config.privateMembershipInterceptor().preSend(message(connect), null);
        assertEquals(owner.toString(), connect.getUser().getName());

        var subscribe = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subscribe.setDestination("/user/queue/membership-card");
        subscribe.setUser(connect.getUser());
        assertDoesNotThrow(() -> config.privateMembershipInterceptor().preSend(message(subscribe), null));
    }

    @Test
    void rejectsAnonymousPrivateSubscriptionsButKeepsLegacyTopics() {
        var subscribe = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subscribe.setDestination("/user/queue/membership-card");
        assertThrows(AccessDeniedException.class, () -> config.privateMembershipInterceptor().preSend(message(subscribe), null));
        var legacy = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        legacy.setDestination("/topic/receptionist-chat");
        assertDoesNotThrow(() -> config.privateMembershipInterceptor().preSend(message(legacy), null));
    }

    @Test
    void rejectsRawQueueOtherUserWildcardAndClientPublishing() {
        for (String destination : new String[]{"/queue/membership-card-userOther", "/queue/**", "/user/other/queue/membership-card", "/user/**"}) {
            var subscribe = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
            subscribe.setUser(() -> "my-account");
            subscribe.setDestination(destination);
            assertThrows(AccessDeniedException.class, () -> config.privateMembershipInterceptor().preSend(message(subscribe), null));
        }
        var send = StompHeaderAccessor.create(StompCommand.SEND);
        send.setUser(() -> "my-account");
        send.setDestination("/user/queue/membership-card");
        assertThrows(AccessDeniedException.class, () -> config.privateMembershipInterceptor().preSend(message(send), null));
    }

    @Test
    void rejectsInvalidTokenAndRefreshToken() {
        when(jwt.parseClaims("invalid")).thenThrow(new IllegalArgumentException());
        var invalid = StompHeaderAccessor.create(StompCommand.CONNECT);
        invalid.setNativeHeader("Authorization", "Bearer invalid");
        assertThrows(AccessDeniedException.class, () -> config.privateMembershipInterceptor().preSend(message(invalid), null));
        Claims claims = mock(Claims.class);
        when(jwt.parseClaims("refresh")).thenReturn(claims);
        when(claims.get("type", String.class)).thenReturn("refresh");
        var refresh = StompHeaderAccessor.create(StompCommand.CONNECT);
        refresh.setNativeHeader("Authorization", "Bearer refresh");
        assertThrows(AccessDeniedException.class, () -> config.privateMembershipInterceptor().preSend(message(refresh), null));
    }
}
