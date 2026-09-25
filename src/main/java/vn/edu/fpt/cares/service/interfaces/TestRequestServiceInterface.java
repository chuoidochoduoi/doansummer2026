package vn.edu.fpt.cares.service.interfaces;

import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.testrequest.TestRequestCancelRequest;
import vn.edu.fpt.cares.dto.testrequest.TestRequestResponse;
import vn.edu.fpt.cares.dto.testrequest.TestRequestCreateRequest;
import vn.edu.fpt.cares.dto.testrequest.TestRequestUpdateRequest;
import vn.edu.fpt.cares.dto.testresult.TestResultResponse;
import vn.edu.fpt.cares.dto.testresult.TestResultCreateRequest;
import vn.edu.fpt.cares.dto.testresult.TestResultUpdateRequest;
import vn.edu.fpt.cares.model.TestRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Pageable;
import vn.edu.fpt.cares.enums.TestRequestStatus;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/** Service interface for TestRequest management. */
public interface TestRequestServiceInterface {
    PageResponse<TestRequestResponse> search(UUID recordId, UUID departmentId,
                                              TestRequestStatus status, String search,
                                              java.time.LocalDate workDate,
                                              Pageable pageable);
    TestRequestResponse get(UUID id);
    TestRequestResponse create(TestRequestCreateRequest req);
    TestRequestResponse update(UUID id, TestRequestUpdateRequest req);
    void delete(UUID id);
    TestRequest findById(UUID id);
    TestResultResponse getResult(UUID testRequestId);
    TestResultResponse createResult(UUID testRequestId, TestResultCreateRequest req);
    TestResultResponse updateResult(UUID testRequestId, TestResultUpdateRequest req);
    TestResultResponse completeResult(UUID testRequestId, TestResultCreateRequest req);

    /** Upload ket qua xet nghiem, luu file va tra ve URL. */
    String uploadResultFile(UUID testRequestId, MultipartFile file) throws IOException;

    /** Huy yeu cau xet nghiem - chi cho PENDING hoac IN_PROGRESS. */
    TestRequestResponse cancel(UUID id, TestRequestCancelRequest req);

    /** Tim TestRequest theo InvoiceItem (traceability: Invoice -> InvoiceItem -> TestRequest). */
    List<TestRequestResponse> findByInvoiceItem(UUID itemId);

    /** Tim TestRequest theo Invoice (traceability: Invoice -> InvoiceItem -> TestRequest). */
    List<TestRequestResponse> findByInvoice(UUID invoiceId);
}
