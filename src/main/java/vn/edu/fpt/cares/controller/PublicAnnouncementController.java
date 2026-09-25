package vn.edu.fpt.cares.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.aop.Auditable;
import vn.edu.fpt.cares.dto.announcement.PublicAnnouncementRequest;
import vn.edu.fpt.cares.dto.announcement.PublicAnnouncementResponse;
import vn.edu.fpt.cares.enums.AuditAction;
import vn.edu.fpt.cares.service.PublicAnnouncementService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PublicAnnouncementController {
    private final PublicAnnouncementService service;

    @GetMapping("/api/public/announcements")
    public List<PublicAnnouncementResponse> visible() {
        return service.listVisible();
    }

    @GetMapping("/api/v1/public-announcements")
    @PreAuthorize("hasAnyRole('ADMIN','CLINIC_MANAGER')")
    public List<PublicAnnouncementResponse> listAll() {
        return service.listAll();
    }

    @PostMapping("/api/v1/public-announcements")
    @PreAuthorize("hasAnyRole('ADMIN','CLINIC_MANAGER')")
    @Auditable(action = AuditAction.CREATE, entityName = "PublicAnnouncement", description = "Tạo thông báo công khai")
    public PublicAnnouncementResponse create(@Valid @RequestBody PublicAnnouncementRequest request) {
        return service.create(request);
    }

    @PutMapping("/api/v1/public-announcements/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CLINIC_MANAGER')")
    @Auditable(action = AuditAction.UPDATE, entityName = "PublicAnnouncement", idParamName = "id", description = "Cập nhật thông báo công khai")
    public PublicAnnouncementResponse update(@PathVariable UUID id,
                                             @Valid @RequestBody PublicAnnouncementRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/api/v1/public-announcements/{id}/publication")
    @PreAuthorize("hasAnyRole('ADMIN','CLINIC_MANAGER')")
    @Auditable(action = AuditAction.STATUS_CHANGE, entityName = "PublicAnnouncement", idParamName = "id", description = "Đổi trạng thái thông báo công khai")
    public PublicAnnouncementResponse setPublication(@PathVariable UUID id,
                                                     @RequestParam boolean published) {
        return service.setPublished(id, published);
    }

    @DeleteMapping("/api/v1/public-announcements/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','CLINIC_MANAGER')")
    @Auditable(action = AuditAction.DELETE, entityName = "PublicAnnouncement", idParamName = "id", description = "Xóa thông báo công khai")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}


