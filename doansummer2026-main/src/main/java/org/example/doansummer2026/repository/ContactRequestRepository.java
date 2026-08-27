package org.example.doansummer2026.repository;

import org.example.doansummer2026.enums.ContactRequestStatus;
import org.example.doansummer2026.model.ContactRequest;
import org.example.doansummer2026.model.StaffInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ContactRequestRepository extends JpaRepository<ContactRequest, UUID>,
        JpaSpecificationExecutor<ContactRequest> {

    long countByPhoneAndCreatedAtAfter(String phone, LocalDateTime createdAfter);

    long countByStatus(ContactRequestStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ContactRequest request
               SET request.status = :processingStatus,
                   request.assignedStaff = :staff,
                   request.acceptedAt = :acceptedAt
             WHERE request.contactRequestId = :id
               AND request.status = :newStatus
               AND request.deleted = false
            """)
    int acceptIfNew(@Param("id") UUID id,
                    @Param("staff") StaffInfo staff,
                    @Param("acceptedAt") LocalDateTime acceptedAt,
                    @Param("newStatus") ContactRequestStatus newStatus,
                    @Param("processingStatus") ContactRequestStatus processingStatus);
}
