package org.example.doansummer2026.service.interfaces;

import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.testRequest.TestRequestResponse;
import org.example.doansummer2026.dto.testRequest.TestRequestCreateRequest;
import org.example.doansummer2026.dto.testRequest.TestRequestUpdateRequest;
import org.example.doansummer2026.dto.testResult.TestResultResponse;
import org.example.doansummer2026.dto.testResult.TestResultCreateRequest;
import org.example.doansummer2026.dto.testResult.TestResultUpdateRequest;
import org.example.doansummer2026.model.TestRequest;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

/** Service interface for TestRequest management. */
public interface TestRequestServiceInterface {
    PageResponse<TestRequestResponse> search(UUID recordId, UUID departmentId,
                                            org.example.doansummer2026.enums.TestRequestStatus status, Pageable pageable);
    TestRequestResponse get(UUID id);
    TestRequestResponse create(TestRequestCreateRequest req);
    TestRequestResponse update(UUID id, TestRequestUpdateRequest req);
    void delete(UUID id);
    TestRequest findById(UUID id);
    TestResultResponse getResult(UUID testRequestId);
    TestResultResponse createResult(UUID testRequestId, TestResultCreateRequest req);
    TestResultResponse updateResult(UUID testRequestId, TestResultUpdateRequest req);
}