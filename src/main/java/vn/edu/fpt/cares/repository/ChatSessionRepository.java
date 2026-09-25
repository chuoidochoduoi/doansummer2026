package vn.edu.fpt.cares.repository;

import vn.edu.fpt.cares.model.ChatSession;
import vn.edu.fpt.cares.enums.ChatSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {
    List<ChatSession> findByCustomer_ProfileId(UUID customerId);
    List<ChatSession> findByStatus(ChatSessionStatus status);
}
