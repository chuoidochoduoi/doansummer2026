package vn.edu.fpt.cares.controller;

import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.enums.Role;
import vn.edu.fpt.cares.exception.BadRequestException;
import vn.edu.fpt.cares.exception.ResourceNotFoundException;
import vn.edu.fpt.cares.model.Account;
import vn.edu.fpt.cares.model.TestResult;
import vn.edu.fpt.cares.repository.TestResultRepository;
import vn.edu.fpt.cares.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/test-results")
@RequiredArgsConstructor
public class TestResultFileController {

    private final TestResultRepository resultRepository;
    private final AuthService authService;
    private final vn.edu.fpt.cares.repository.StaffInfoRepository staffInfoRepository;
    private final vn.edu.fpt.cares.service.FamilyAccessService familyAccessService;

    @Value("${app.upload.root:uploads}")
    private String uploadRoot;

    @GetMapping("/{resultId}/file")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> viewFile(@PathVariable UUID resultId,
                                           @RequestParam(defaultValue = "inline") String disposition) {
        TestResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phiếu kết quả"));
        verifyAccess(result, authService.currentAccount());

        String stored = result.getImageUrl();
        if (stored == null || stored.isBlank()) {
            throw new ResourceNotFoundException("Phiếu kết quả chưa có tệp PDF");
        }
        String fileName = Paths.get(stored).getFileName().toString();
        Path root = Paths.get(uploadRoot).toAbsolutePath().normalize();
        Path resultDirectory = root.resolve("test-results").normalize();
        Path file = resultDirectory.resolve(fileName).normalize();
        if (!file.startsWith(resultDirectory) || !Files.isRegularFile(file) || !Files.isReadable(file)) {
            throw new ResourceNotFoundException("Tệp kết quả không tồn tại");
        }

        byte[] content;
        try {
            content = Files.readAllBytes(file);
        } catch (IOException ex) {
            throw new BadRequestException("Không thể đọc tệp kết quả. Vui lòng tải lại tệp PDF");
        }
        ContentDisposition contentDisposition = "attachment".equalsIgnoreCase(disposition)
                ? ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build()
                : ContentDisposition.inline().filename(fileName, StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(content.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(content);
    }

    private void verifyAccess(TestResult result, Account account) {
        if (account == null || result.getTestRequest() == null) {
            throw new AccessDeniedException("Không có quyền xem phiếu kết quả này");
        }
        var request = result.getTestRequest();
        var record = request.getMedicalRecord();
        var visit = record != null ? record.getVisit() : null;
        var customer = visit != null ? visit.getCustomer() : null;
        if (account.getRole() == Role.CUSTOMER) {
            if (customer == null) {
                throw new AccessDeniedException("Không có quyền xem phiếu kết quả này");
            }
            try {
                familyAccessService.resolveReadableProfile(account.getAccountId(),
                        customer == null ? null : customer.getProfileId());
            } catch (RuntimeException ex) {
                throw new AccessDeniedException("Không có quyền xem phiếu kết quả này");
            }
            return;
        }

        var role = authService.getCurrentSystemRole();
        if (role == vn.edu.fpt.cares.enums.SystemRole.ADMIN) return;
        if (role == null || (!role.isDoctor()
                && role != vn.edu.fpt.cares.enums.SystemRole.NURSE)) {
            throw new AccessDeniedException("Chỉ nhân viên chuyên môn liên quan mới được xem phiếu kết quả");
        }

        UUID staffId = authService.currentStaffId();
        var department = request.getPerformingDepartment();
        var staff = staffId == null ? null : staffInfoRepository.findById(staffId).orElse(null);
        boolean assignedToDepartment = staff != null && department != null
                && staff.getDepartment() != null
                && department.getDepartmentId().equals(staff.getDepartment().getDepartmentId());
        boolean orderingDoctor = staffId != null && request.getRequestedBy() != null
                && staffId.equals(request.getRequestedBy().getStaffId());
        boolean recordDoctor = staffId != null && record != null && record.getDoctor() != null
                && staffId.equals(record.getDoctor().getStaffId());
        if (!assignedToDepartment && !orderingDoctor && !recordDoctor) {
            throw new AccessDeniedException("Không có quyền xem phiếu kết quả này");
        }
    }
}
