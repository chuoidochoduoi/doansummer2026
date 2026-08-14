package org.example.doansummer2026.controller;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.enums.Role;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.model.TestResult;
import org.example.doansummer2026.repository.TestResultRepository;
import org.example.doansummer2026.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/test-results")
@RequiredArgsConstructor
public class TestResultFileController {

    private final TestResultRepository resultRepository;
    private final AuthService authService;

    @Value("${app.upload.root:uploads}")
    private String uploadRoot;

    @GetMapping("/{resultId}/file")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> viewFile(@PathVariable UUID resultId,
                                             @RequestParam(defaultValue = "inline") String disposition)
            throws MalformedURLException {
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
        if (!file.startsWith(resultDirectory) || !file.toFile().isFile()) {
            throw new ResourceNotFoundException("Tệp kết quả không tồn tại");
        }

        Resource resource = new UrlResource(file.toUri());
        ContentDisposition contentDisposition = "attachment".equalsIgnoreCase(disposition)
                ? ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build()
                : ContentDisposition.inline().filename(fileName, StandardCharsets.UTF_8).build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(resource);
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
            UUID ownerAccountId = customer != null && customer.getAccount() != null
                    ? customer.getAccount().getAccountId()
                    : null;
            if (!account.getAccountId().equals(ownerAccountId)) {
                throw new AccessDeniedException("Không có quyền xem phiếu kết quả này");
            }
            return;
        }

        var role = authService.getCurrentSystemRole();
        if (role == org.example.doansummer2026.enums.SystemRole.ADMIN) return;
        if (role == null || (!role.isDoctor()
                && role != org.example.doansummer2026.enums.SystemRole.NURSE)) {
            throw new AccessDeniedException("Chỉ nhân viên chuyên môn liên quan mới được xem phiếu kết quả");
        }

        UUID staffId = authService.currentStaffId();
        var department = request.getPerformingDepartment();
        boolean assignedToDepartment = staffId != null && department != null
                && ((department.getHeadDoctor() != null
                        && staffId.equals(department.getHeadDoctor().getStaffId()))
                    || (department.getNurses() != null && department.getNurses().stream()
                        .anyMatch(nurse -> staffId.equals(nurse.getStaffId()))));
        boolean orderingDoctor = staffId != null && request.getRequestedBy() != null
                && staffId.equals(request.getRequestedBy().getStaffId());
        boolean recordDoctor = staffId != null && record != null && record.getDoctor() != null
                && staffId.equals(record.getDoctor().getStaffId());
        if (!assignedToDepartment && !orderingDoctor && !recordDoctor) {
            throw new AccessDeniedException("Không có quyền xem phiếu kết quả này");
        }
    }
}
