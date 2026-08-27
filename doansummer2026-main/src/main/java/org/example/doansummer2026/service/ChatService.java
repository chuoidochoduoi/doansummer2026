package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.enums.ChatSenderType;
import org.example.doansummer2026.enums.ChatSessionStatus;
import org.example.doansummer2026.model.ChatMessage;
import org.example.doansummer2026.model.ChatSession;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.repository.ChatMessageRepository;
import org.example.doansummer2026.repository.ChatSessionRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;
    private final ProfileRepository profileRepo;
    private final RuleBasedBotService botService;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatSession startOrGetActiveSession(UUID customerId) {
        // Tìm session đang active (chưa đóng)
        List<ChatSession> sessions = sessionRepo.findByCustomer_ProfileId(customerId);
        Optional<ChatSession> active = sessions.stream()
                .filter(s -> s.getStatus() != ChatSessionStatus.CLOSED)
                .findFirst();

        if (active.isPresent()) {
            return active.get();
        }

        Profile customer = profileRepo.findById(customerId).orElseThrow(() -> new RuntimeException("Không tìm thấy hồ sơ khách hàng"));
        ChatSession newSession = ChatSession.builder()
                .customer(customer)
                .status(ChatSessionStatus.BOT_HANDLING)
                .build();
        return sessionRepo.save(newSession);
    }

    public ChatSession startOrGetGuestSession(String fullName, String phone) {
        Profile guestProfile = profileRepo.findFirstByPhone(phone).orElseGet(() -> {
            Profile newProfile = Profile.builder()
                    .fullName(fullName)
                    .phone(phone)
                    .build();
            return profileRepo.save(newProfile);
        });

        // Tìm xem khách này có session chưa đóng không
        List<ChatSession> sessions = sessionRepo.findByCustomer_ProfileId(guestProfile.getProfileId());
        Optional<ChatSession> active = sessions.stream()
                .filter(s -> s.getStatus() != ChatSessionStatus.CLOSED)
                .findFirst();

        if (active.isPresent()) {
            return active.get();
        }

        ChatSession newSession = ChatSession.builder()
                .customer(guestProfile)
                .status(ChatSessionStatus.BOT_HANDLING)
                .build();
        return sessionRepo.save(newSession);
    }

    public void processCustomerMessage(UUID sessionId, UUID customerId, String content) {
        ChatSession session = sessionRepo.findById(sessionId).orElseThrow();
        
        // 1. Lưu tin nhắn của khách hàng
        ChatMessage customerMsg = ChatMessage.builder()
                .session(session)
                .senderType(ChatSenderType.CUSTOMER)
                .senderId(customerId)
                .content(content)
                .build();
        messageRepo.save(customerMsg);

        // 2. Broadcast tin nhắn này tới Receptionist (nếu đang chờ hoặc in-progress)
        if (session.getStatus() == ChatSessionStatus.WAITING_FOR_AGENT || session.getStatus() == ChatSessionStatus.IN_PROGRESS) {
            messagingTemplate.convertAndSend("/topic/receptionist-chat", "NEW_MESSAGE");
        }

        // Luôn broadcast tới topic của session để update UI cho cả Khách và Lễ tân
        messagingTemplate.convertAndSend("/topic/chat-" + sessionId, "NEW_MESSAGE");

        // 3. Nếu đang được BOT_HANDLING, gọi bot
        if (session.getStatus() == ChatSessionStatus.BOT_HANDLING) {
            String botReply = botService.getBotResponse(content);
            
            if ("[HANDOVER]".equals(botReply)) {
                // Đổi trạng thái sang chờ nhân viên
                session.setStatus(ChatSessionStatus.WAITING_FOR_AGENT);
                sessionRepo.save(session);
                
                ChatMessage botMsg = ChatMessage.builder()
                        .session(session)
                        .senderType(ChatSenderType.BOT)
                        .content("Yêu cầu của bạn đã được chuyển đến Lễ tân. Vui lòng đợi trong giây lát.")
                        .build();
                messageRepo.save(botMsg);
                
                // Báo cho khách hàng
                messagingTemplate.convertAndSend("/topic/chat-" + sessionId, "NEW_MESSAGE");
                // Báo cho toàn bộ receptionist có chat mới cần hỗ trợ
                messagingTemplate.convertAndSend("/topic/receptionist-chat", "NEW_CHAT_REQUEST");
            } else {
                // Lưu tin nhắn bot trả lời
                ChatMessage botMsg = ChatMessage.builder()
                        .session(session)
                        .senderType(ChatSenderType.BOT)
                        .content(botReply)
                        .build();
                messageRepo.save(botMsg);
                
                // Broadcast cho khách hàng
                messagingTemplate.convertAndSend("/topic/chat-" + sessionId, "NEW_MESSAGE");
            }
        }
    }

    public void processReceptionistMessage(UUID sessionId, UUID receptionistId, String content) {
        ChatSession session = sessionRepo.findById(sessionId).orElseThrow();
        
        if (session.getStatus() == ChatSessionStatus.WAITING_FOR_AGENT) {
            session.setStatus(ChatSessionStatus.IN_PROGRESS);
            session.setAssignedReceptionistId(receptionistId);
            sessionRepo.save(session);
            // Notify other receptionists that this chat is taken
            messagingTemplate.convertAndSend("/topic/receptionist-chat", "CHAT_ACCEPTED");
        }

        ChatMessage repMsg = ChatMessage.builder()
                .session(session)
                .senderType(ChatSenderType.RECEPTIONIST)
                .senderId(receptionistId)
                .content(content)
                .build();
        messageRepo.save(repMsg);

        // Broadcast tới khách hàng
        messagingTemplate.convertAndSend("/topic/chat-" + sessionId, "NEW_MESSAGE");
    }

    public void closeSession(UUID sessionId) {
        ChatSession session = sessionRepo.findById(sessionId).orElseThrow();
        session.setStatus(ChatSessionStatus.CLOSED);
        sessionRepo.save(session);
        messagingTemplate.convertAndSend("/topic/chat-" + sessionId, "SESSION_CLOSED");
        messagingTemplate.convertAndSend("/topic/receptionist-chat", "SESSION_CLOSED");
    }

    public List<ChatSession> getActiveSessionsForReceptionist() {
        List<ChatSession> list = sessionRepo.findByStatus(ChatSessionStatus.WAITING_FOR_AGENT);
        list.addAll(sessionRepo.findByStatus(ChatSessionStatus.IN_PROGRESS));
        return list;
    }

    public List<ChatSession> getClosedSessionsForReceptionist() {
        return sessionRepo.findByStatus(ChatSessionStatus.CLOSED);
    }

    public List<ChatMessage> getMessages(UUID sessionId) {
        return messageRepo.findBySession_SessionIdOrderByCreatedAtAsc(sessionId);
    }

    public ChatSession getSession(UUID sessionId) {
        return sessionRepo.findById(sessionId).orElseThrow();
    }
}
