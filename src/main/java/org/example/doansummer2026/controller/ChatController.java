package org.example.doansummer2026.controller;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.config.JwtService;
import org.example.doansummer2026.enums.Role;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.model.ChatMessage;
import org.example.doansummer2026.model.ChatSession;
import org.example.doansummer2026.repository.AccountRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.example.doansummer2026.service.ChatService;
import org.example.doansummer2026.model.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final AccountRepository accountRepo;
    private final ProfileRepository profileRepo;

    private String getUsername(org.springframework.security.core.Authentication auth) {
        if (auth.getPrincipal() instanceof Map<?, ?> map) {
            return (String) map.get("username");
        }
        return auth.getName();
    }

    // Helper để lấy current user id - do JwtAuthFilter set Auth
    // Tuy nhiên trong controller đơn giản nhất là pass customerId hoặc receptionistId
    // Trong Spring Security có thể dùng Authentication
    
    @PostMapping("/session")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> startOrGetSession(org.springframework.security.core.Authentication auth) {
        // Lấy thông tin user
        String username = getUsername(auth);
        Account acc = accountRepo.findFirstByUsername(username).orElseThrow();
        Profile profile = profileRepo.findFirstByAccount_AccountId(acc.getAccountId()).orElseThrow();
        ChatSession session = chatService.startOrGetActiveSession(profile.getProfileId());
        return ResponseEntity.ok(Map.of(
            "sessionId", session.getSessionId(), 
            "status", session.getStatus(),
            "profileId", profile.getProfileId()
        ));
    }

    @PostMapping("/guest/session")
    public ResponseEntity<?> startOrGetGuestSession(@RequestBody Map<String, String> body) {
        String fullName = body.get("fullName");
        String phone = body.get("phone");
        if (fullName == null || phone == null || fullName.trim().isEmpty() || phone.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Thiếu thông tin tên hoặc số điện thoại");
        }
        
        ChatSession session = chatService.startOrGetGuestSession(fullName.trim(), phone.trim());
        return ResponseEntity.ok(Map.of(
            "sessionId", session.getSessionId(),
            "status", session.getStatus(),
            "guestProfileId", session.getCustomer().getProfileId()
        ));
    }

    @GetMapping("/sessions/active")
    @PreAuthorize("hasRole('RECEPTIONIST')")
    public ResponseEntity<?> getActiveSessionsForReceptionist() {
        List<ChatSession> sessions = chatService.getActiveSessionsForReceptionist();
        var res = sessions.stream().map(s -> Map.of(
            "sessionId", s.getSessionId(),
            "customerName", s.getCustomer().getFullName(),
            "status", s.getStatus(),
            "assignedReceptionistId", s.getAssignedReceptionistId() != null ? s.getAssignedReceptionistId() : "",
            "updatedAt", s.getUpdatedAt() != null ? s.getUpdatedAt() : s.getCreatedAt()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/sessions/history")
    @PreAuthorize("hasRole('RECEPTIONIST')")
    public ResponseEntity<?> getClosedSessionsForReceptionist() {
        List<ChatSession> sessions = chatService.getClosedSessionsForReceptionist();
        var res = sessions.stream().map(s -> Map.of(
            "sessionId", s.getSessionId(),
            "customerName", s.getCustomer().getFullName(),
            "status", s.getStatus(),
            "assignedReceptionistId", s.getAssignedReceptionistId() != null ? s.getAssignedReceptionistId() : "",
            "updatedAt", s.getUpdatedAt() != null ? s.getUpdatedAt() : s.getCreatedAt()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<?> getMessages(@PathVariable UUID sessionId) {
        List<ChatMessage> messages = chatService.getMessages(sessionId);
        var res = messages.stream().map(m -> Map.of(
            "messageId", m.getMessageId(),
            "senderType", m.getSenderType(),
            "content", m.getContent(),
            "createdAt", m.getCreatedAt()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{sessionId}/status")
    public ResponseEntity<?> getSessionStatus(@PathVariable UUID sessionId) {
        ChatSession session = chatService.getSession(sessionId);
        return ResponseEntity.ok(Map.of("status", session.getStatus()));
    }

    @PostMapping("/{sessionId}/messages/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<?> sendCustomerMessage(
            @PathVariable UUID sessionId,
            @RequestBody Map<String, String> body,
            org.springframework.security.core.Authentication auth) {
        String username = getUsername(auth);
        Account acc = accountRepo.findFirstByUsername(username).orElseThrow();
        Profile profile = profileRepo.findFirstByAccount_AccountId(acc.getAccountId()).orElseThrow();
        chatService.processCustomerMessage(sessionId, profile.getProfileId(), body.get("content"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/guest/{sessionId}/messages")
    public ResponseEntity<?> sendGuestMessage(
            @PathVariable UUID sessionId,
            @RequestBody Map<String, String> body) {
        String content = body.get("content");
        String guestProfileIdStr = body.get("guestProfileId");
        
        if (content == null || content.trim().isEmpty() || guestProfileIdStr == null) {
            return ResponseEntity.badRequest().body("Thiếu nội dung tin nhắn hoặc guestProfileId");
        }
        
        chatService.processCustomerMessage(sessionId, UUID.fromString(guestProfileIdStr), content.trim());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{sessionId}/messages/receptionist")
    @PreAuthorize("hasRole('RECEPTIONIST')")
    public ResponseEntity<?> sendReceptionistMessage(
            @PathVariable UUID sessionId,
            @RequestBody Map<String, String> body,
            org.springframework.security.core.Authentication auth) {
        String username = getUsername(auth);
        Account acc = accountRepo.findFirstByUsername(username).orElseThrow();
        // Cần truyền receptionistId (có thể lấy từ acc.getStaffInfo().getStaffId() nếu có mapping)
        // Tạm thời truyền accountId hoặc UUID ngẫu nhiên nếu không cần track chặt chẽ
        UUID staffId = acc.getAccountId(); 
        chatService.processReceptionistMessage(sessionId, staffId, body.get("content"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{sessionId}/close")
    @PreAuthorize("hasRole('RECEPTIONIST')")
    public ResponseEntity<?> closeSession(@PathVariable UUID sessionId) {
        chatService.closeSession(sessionId);
        return ResponseEntity.ok().build();
    }
}
