package org.example.doansummer2026.dto.contact;

import org.example.doansummer2026.enums.ContactRequestStatus;
import org.example.doansummer2026.model.ContactRequest;

import java.time.LocalDateTime;
import java.util.UUID;

public record ContactRequestResponse(
        UUID contactRequestId,
        String requestCode,
        String fullName,
        String phone,
        String email,
        String subject,
        String message,
        ContactRequestStatus status,
        UUID assignedStaffId,
        String assignedStaffName,
        String internalNote,
        LocalDateTime acceptedAt,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ContactRequestResponse from(ContactRequest request) {
        var staff = request.getAssignedStaff();
        return new ContactRequestResponse(
                request.getContactRequestId(), request.getRequestCode(), request.getFullName(),
                request.getPhone(), request.getEmail(), request.getSubject(), request.getMessage(),
                request.getStatus(), staff == null ? null : staff.getStaffId(),
                staff == null || staff.getProfile() == null ? null : staff.getProfile().getFullName(),
                request.getInternalNote(), request.getAcceptedAt(), request.getCompletedAt(),
                request.getCreatedAt(), request.getUpdatedAt());
    }
}
