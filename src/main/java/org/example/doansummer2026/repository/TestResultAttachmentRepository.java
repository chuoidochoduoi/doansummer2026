package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.TestResultAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TestResultAttachmentRepository extends JpaRepository<TestResultAttachment, UUID> {
    List<TestResultAttachment> findByRevision_RevisionIdOrderByDisplayOrder(UUID revisionId);
    long countByRevision_RevisionId(UUID revisionId);
}
