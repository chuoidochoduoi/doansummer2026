package vn.edu.fpt.cares.repository;

import vn.edu.fpt.cares.model.Notification;
import vn.edu.fpt.cares.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID>, JpaSpecificationExecutor<Notification> {

    default Page<Notification> search(UUID recipientId, NotificationStatus status,
                                       Pageable pageable) {
        Specification<Notification> spec = (root, query, cb) -> cb.conjunction();

        if (recipientId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("recipient").get("profileId"), recipientId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }

        return findAll(spec, pageable);
    }

    long countByRecipient_ProfileIdAndStatus(UUID recipientId, NotificationStatus status);

    List<Notification> findAllByRelatedEntityAndRelatedEntityIdAndStatusOrderByCreatedAtAsc(
            String relatedEntity, UUID relatedEntityId, NotificationStatus status);

    List<Notification> findAllByRelatedEntityAndStatusOrderByCreatedAtAsc(
            String relatedEntity, NotificationStatus status);
}



