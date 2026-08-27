package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.TestResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, UUID> {

    Optional<TestResult> findByTestRequest_TestRequestId(UUID testRequestId);
}



