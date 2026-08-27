package org.example.doansummer2026.repository;

import org.example.doansummer2026.enums.TestResultRevisionStatus;
import org.example.doansummer2026.model.TestResultRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TestResultRevisionRepository extends JpaRepository<TestResultRevision, UUID> {
    List<TestResultRevision> findByTestResult_ResultIdOrderByRevisionNoDesc(UUID resultId);
    Optional<TestResultRevision> findFirstByTestResult_ResultIdOrderByRevisionNoDesc(UUID resultId);
    Optional<TestResultRevision> findFirstByTestResult_ResultIdAndStatusOrderByRevisionNoDesc(UUID resultId, TestResultRevisionStatus status);
}
