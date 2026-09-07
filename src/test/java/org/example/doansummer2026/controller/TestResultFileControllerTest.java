package org.example.doansummer2026.controller;

import org.example.doansummer2026.enums.Role;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.*;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.example.doansummer2026.repository.TestResultAttachmentRepository;
import org.example.doansummer2026.repository.TestResultRepository;
import org.example.doansummer2026.service.AuthService;
import org.example.doansummer2026.service.FamilyAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestResultFileControllerTest {
    @Mock TestResultRepository resultRepository;
    @Mock TestResultAttachmentRepository attachmentRepository;
    @Mock AuthService authService;
    @Mock StaffInfoRepository staffInfoRepository;
    @Mock FamilyAccessService familyAccessService;
    @TempDir Path temp;
    private TestResultFileController controller;
    private UUID resultId;
    private UUID accountId;
    private Profile customer;
    private TestRequest request;
    private TestResult result;

    @BeforeEach
    void setUp() {
        controller = new TestResultFileController(resultRepository, attachmentRepository, authService,
                staffInfoRepository, familyAccessService);
        ReflectionTestUtils.setField(controller, "uploadRoot", temp.toString());
        resultId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        customer = Profile.builder().profileId(UUID.randomUUID()).build();
        MedicalRecord record = MedicalRecord.builder()
                .visit(CustomerVisit.builder().customer(customer).build()).build();
        request = TestRequest.builder().testRequestId(UUID.randomUUID()).medicalRecord(record).build();
        result = TestResult.builder().resultId(resultId).testRequest(request).imageUrl("result.pdf").build();
    }

    @Test
    void missingResultAndMissingPdfAreReported() {
        when(resultRepository.findById(resultId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> controller.viewFile(resultId, "inline"));

        Account account = customerAccount();
        when(resultRepository.findById(resultId)).thenReturn(Optional.of(result));
        when(authService.currentAccount()).thenReturn(account);
        result.setImageUrl(" ");
        assertThrows(ResourceNotFoundException.class, () -> controller.viewFile(resultId, "inline"));
    }

    @Test
    void customerCanViewInlinePdfThroughFamilyAccess() throws Exception {
        Path dir = Files.createDirectories(temp.resolve("test-results"));
        Files.write(dir.resolve("result.pdf"), new byte[]{1, 2, 3});
        Account account = customerAccount();
        when(resultRepository.findById(resultId)).thenReturn(Optional.of(result));
        when(authService.currentAccount()).thenReturn(account);

        var response = controller.viewFile(resultId, "inline");

        assertArrayEquals(new byte[]{1, 2, 3}, response.getBody());
        assertEquals(3, response.getHeaders().getContentLength());
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).startsWith("inline"));
        verify(familyAccessService).resolveReadableProfile(accountId, customer.getProfileId());
    }

    @Test
    void customerCanDownloadPdfAsAttachment() throws Exception {
        Path dir = Files.createDirectories(temp.resolve("test-results"));
        Files.write(dir.resolve("result.pdf"), new byte[]{9});
        when(resultRepository.findById(resultId)).thenReturn(Optional.of(result));
        when(authService.currentAccount()).thenReturn(customerAccount());
        var response = controller.viewFile(resultId, "ATTACHMENT");
        assertTrue(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).startsWith("attachment"));
    }

    @Test
    void missingAccountRequestOrCustomerIsDenied() {
        when(resultRepository.findById(resultId)).thenReturn(Optional.of(result));
        when(authService.currentAccount()).thenReturn(null);
        assertThrows(AccessDeniedException.class, () -> controller.viewFile(resultId, "inline"));

        Account customerAccount = customerAccount();
        when(authService.currentAccount()).thenReturn(customerAccount);
        result.setTestRequest(null);
        assertThrows(AccessDeniedException.class, () -> controller.viewFile(resultId, "inline"));

        result.setTestRequest(TestRequest.builder().medicalRecord(MedicalRecord.builder()
                .visit(CustomerVisit.builder().customer(null).build()).build()).build());
        assertThrows(AccessDeniedException.class, () -> controller.viewFile(resultId, "inline"));
    }

    @Test
    void familyAccessFailureIsConvertedToAccessDenied() {
        when(resultRepository.findById(resultId)).thenReturn(Optional.of(result));
        when(authService.currentAccount()).thenReturn(customerAccount());
        when(familyAccessService.resolveReadableProfile(accountId, customer.getProfileId()))
                .thenThrow(new ResourceNotFoundException("outside family"));
        assertThrows(AccessDeniedException.class, () -> controller.viewFile(resultId, "inline"));
    }

    @Test
    void adminCanViewButNonClinicalStaffCannot() throws Exception {
        Path dir = Files.createDirectories(temp.resolve("test-results"));
        Files.write(dir.resolve("result.pdf"), new byte[]{4});
        Account staffAccount = staffAccount();
        when(resultRepository.findById(resultId)).thenReturn(Optional.of(result));
        when(authService.currentAccount()).thenReturn(staffAccount);
        when(authService.getCurrentSystemRole()).thenReturn(SystemRole.ADMIN);
        assertNotNull(controller.viewFile(resultId, "inline").getBody());

        when(authService.getCurrentSystemRole()).thenReturn(SystemRole.CASHIER);
        assertThrows(AccessDeniedException.class, () -> controller.viewFile(resultId, "inline"));
    }

    @Test
    void relatedClinicalStaffCanViewAndUnrelatedStaffCannot() throws Exception {
        Path dir = Files.createDirectories(temp.resolve("test-results"));
        Files.write(dir.resolve("result.pdf"), new byte[]{5});
        UUID staffId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        Department department = Department.builder().departmentId(departmentId).build();
        request.setPerformingDepartment(department);
        when(resultRepository.findById(resultId)).thenReturn(Optional.of(result));
        when(authService.currentAccount()).thenReturn(staffAccount());
        when(authService.getCurrentSystemRole()).thenReturn(SystemRole.DOCTOR);
        when(authService.currentStaffId()).thenReturn(staffId);

        StaffInfo assigned = StaffInfo.builder().staffId(staffId)
                .department(Department.builder().departmentId(departmentId).build()).build();
        when(staffInfoRepository.findById(staffId)).thenReturn(Optional.of(assigned));
        assertNotNull(controller.viewFile(resultId, "inline").getBody());

        StaffInfo unrelated = StaffInfo.builder().staffId(staffId)
                .department(Department.builder().departmentId(UUID.randomUUID()).build()).build();
        when(staffInfoRepository.findById(staffId)).thenReturn(Optional.of(unrelated));
        assertThrows(AccessDeniedException.class, () -> controller.viewFile(resultId, "inline"));

        request.setRequestedBy(StaffInfo.builder().staffId(staffId).build());
        assertNotNull(controller.viewFile(resultId, "inline").getBody());

        request.setRequestedBy(null);
        request.getMedicalRecord().setDoctor(StaffInfo.builder().staffId(staffId).build());
        assertNotNull(controller.viewFile(resultId, "inline").getBody());
    }

    @Test
    void nonexistentFileIsReportedEvenWhenStoredNameContainsDirectories() {
        when(resultRepository.findById(resultId)).thenReturn(Optional.of(result));
        when(authService.currentAccount()).thenReturn(customerAccount());
        result.setImageUrl("../../missing.pdf");
        assertThrows(ResourceNotFoundException.class, () -> controller.viewFile(resultId, "inline"));
    }

    @Test
    void attachmentCanBeViewedOrDownloadedAndInvalidPathIsRejected() throws Exception {
        Path dir = Files.createDirectories(temp.resolve("test-results").resolve("attachments"));
        Path file = dir.resolve("report.txt");
        Files.writeString(file, "signed result");
        TestResultRevision revision = TestResultRevision.builder().testResult(result).build();
        UUID attachmentId = UUID.randomUUID();
        TestResultAttachment attachment = TestResultAttachment.builder().attachmentId(attachmentId)
                .revision(revision).storagePath(file.toString()).originalName("kết quả.txt")
                .contentType("text/plain").build();
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));
        when(authService.currentAccount()).thenReturn(customerAccount());

        var inline = controller.viewAttachment(attachmentId, "inline");
        assertEquals("signed result", new String(inline.getBody()));
        assertTrue(inline.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).startsWith("inline"));
        var download = controller.viewAttachment(attachmentId, "attachment");
        assertTrue(download.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).startsWith("attachment"));

        attachment.setStoragePath(temp.resolve("outside.txt").toString());
        assertThrows(ResourceNotFoundException.class,
                () -> controller.viewAttachment(attachmentId, "inline"));
    }

    @Test
    void missingAttachmentIsReported() {
        UUID id = UUID.randomUUID();
        when(attachmentRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> controller.viewAttachment(id, "inline"));
    }

    private Account customerAccount() {
        return Account.builder().accountId(accountId).role(Role.CUSTOMER).build();
    }

    private Account staffAccount() {
        return Account.builder().accountId(accountId).role(Role.STAFF).build();
    }
}
