package vn.edu.fpt.cares.repository;

import vn.edu.fpt.cares.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findBySession_SessionIdOrderByCreatedAtAsc(UUID sessionId);
}
