package org.example.doansummer2026.config;

import org.springframework.context.annotation.Configuration;
import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.repository.AccountRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtService jwtService;
    private final AccountRepository accountRepository;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Prefix cho các topic gửi từ server tới client (ví dụ: /topic/lab-queue)
        config.enableSimpleBroker("/topic", "/queue");
        config.setUserDestinationPrefix("/user");
        // Prefix cho các message gửi từ client lên server (nếu cần)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint để client kết nối tới STOMP (ví dụ: /ws)
        registry.addEndpoint("/ws").setAllowedOriginPatterns("*").withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(privateMembershipInterceptor());
    }

    ChannelInterceptor privateMembershipInterceptor() {
        return new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor == null) return message;
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authorization = accessor.getFirstNativeHeader("Authorization");
                    // Existing public/chat subscriptions remain compatible without a token.
                    if (authorization != null) {
                        try {
                            if (!authorization.startsWith("Bearer ")) throw new IllegalArgumentException();
                            var claims = jwtService.parseClaims(authorization.substring(7));
                            if (!"access".equals(claims.get("type", String.class))) throw new IllegalArgumentException();
                            var account = accountRepository.findFirstByUsername(claims.getSubject()).orElseThrow();
                            if (!Boolean.TRUE.equals(account.getIsActive())
                                    || !account.getAccountId().toString().equals(claims.get("uid", String.class))) {
                                throw new IllegalArgumentException();
                            }
                            // Identity comes from the verified token, never from a client-supplied account ID.
                            accessor.setUser(new UsernamePasswordAuthenticationToken(
                                    account.getAccountId().toString(), null, List.of()));
                        } catch (RuntimeException ex) {
                            throw new AccessDeniedException("Phiên kết nối không hợp lệ hoặc đã hết hạn");
                        }
                    }
                }
                String destination = accessor.getDestination();
                if (destination != null && (destination.startsWith("/queue") || destination.startsWith("/user"))) {
                    // Do not allow raw broker queues, another user's destination or client publication.
                    if (!StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                            || !"/user/queue/membership-card".equals(destination)
                            || accessor.getUser() == null) {
                        throw new AccessDeniedException("Không có quyền truy cập kênh thẻ trả trước");
                    }
                }
                return message;
            }
        };
    }
}
