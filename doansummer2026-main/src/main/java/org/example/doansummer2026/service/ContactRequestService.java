package org.example.doansummer2026.service;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.contact.ContactRequestCreateRequest;
import org.example.doansummer2026.dto.contact.ContactRequestResolveRequest;
import org.example.doansummer2026.dto.contact.ContactRequestResponse;
import org.example.doansummer2026.enums.ContactRequestStatus;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.ContactRequest;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.repository.ContactRequestRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ContactRequestService {

    private final ContactRequestRepository repository;
    private final StaffInfoRepository staffRepository;
    private final AuthService authService;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Value("${app.contact.recipient-email:}")
    private String recipientEmail;

    public ContactRequestResponse create(ContactRequestCreateRequest request) {
        String phone = request.phone().trim();
        if (repository.countByPhoneAndCreatedAtAfter(phone, LocalDateTime.now().minusMinutes(10)) >= 3) {
            throw new BadRequestException("Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau 10 phút");
        }

        ContactRequest entity = ContactRequest.builder()
                .requestCode(generateCode())
                .fullName(request.fullName().trim())
                .phone(phone)
                .email(blankToNull(request.email()))
                .subject(request.subject().trim())
                .message(request.message().trim())
                .status(ContactRequestStatus.NEW)
                .build();
        ContactRequest saved = repository.save(entity);

        String notificationContent = saved.getFullName() + " vừa gửi yêu cầu " + saved.getRequestCode();
        notificationService.notifyStaffByRole(SystemRole.RECEPTIONIST, "Yêu cầu liên hệ mới",
                notificationContent, "ContactRequest", saved.getContactRequestId());
        notificationService.notifyStaffByRole(SystemRole.CLINIC_MANAGER, "Yêu cầu liên hệ mới",
                notificationContent, "ContactRequest", saved.getContactRequestId());

        if (recipientEmail != null && !recipientEmail.isBlank()) {
            try {
                emailService.sendContactRequestEmail(recipientEmail, saved.getRequestCode(), saved.getFullName(),
                        saved.getPhone(), saved.getEmail(), saved.getSubject(), saved.getMessage());
            } catch (RuntimeException ex) {
                log.warn("Yêu cầu {} đã được lưu nhưng email thông báo gửi thất bại", saved.getRequestCode());
            }
        }
        return ContactRequestResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ContactRequestResponse> search(String search, ContactRequestStatus status,
                                                        LocalDate fromDate, LocalDate toDate,
                                                        Pageable pageable) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("Ngày bắt đầu không được sau ngày kết thúc");
        }
        String keyword = search == null || search.isBlank()
                ? null : "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        Specification<ContactRequest> specification = (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            predicates.add(cb.isFalse(root.get("deleted")));
            if (keyword != null) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("requestCode")), keyword),
                        cb.like(cb.lower(root.get("fullName")), keyword),
                        cb.like(cb.lower(root.get("phone")), keyword),
                        cb.like(cb.lower(root.get("email")), keyword)
                ));
            }
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (fromDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate.atStartOfDay()));
            if (toDate != null) predicates.add(cb.lessThan(root.get("createdAt"), toDate.plusDays(1).atStartOfDay()));
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Page<ContactRequest> page = repository.findAll(specification, pageable);
        return PageResponse.from(page, ContactRequestResponse::from);
    }

    @Transactional(readOnly = true)
    public ContactRequestResponse get(UUID id) {
        return ContactRequestResponse.from(findById(id));
    }

    @Transactional(readOnly = true)
    public long countNew() {
        return repository.countByStatus(ContactRequestStatus.NEW);
    }

    public ContactRequestResponse accept(UUID id) {
        StaffInfo currentStaff = currentStaff();
        int updated = repository.acceptIfNew(id, currentStaff, LocalDateTime.now(),
                ContactRequestStatus.NEW, ContactRequestStatus.PROCESSING);
        if (updated == 0) {
            ContactRequest current = findById(id);
            String assignee = current.getAssignedStaff() == null || current.getAssignedStaff().getProfile() == null
                    ? "nhân viên khác" : current.getAssignedStaff().getProfile().getFullName();
            throw new ConflictException("Yêu cầu đã được " + assignee + " tiếp nhận hoặc không còn ở trạng thái mới");
        }
        return ContactRequestResponse.from(findById(id));
    }

    public ContactRequestResponse complete(UUID id, ContactRequestResolveRequest request) {
        return resolve(id, request, ContactRequestStatus.COMPLETED);
    }

    public ContactRequestResponse cancel(UUID id, ContactRequestResolveRequest request) {
        return resolve(id, request, ContactRequestStatus.CANCELLED);
    }

    private ContactRequestResponse resolve(UUID id, ContactRequestResolveRequest request,
                                           ContactRequestStatus targetStatus) {
        ContactRequest entity = findById(id);
        if (entity.getStatus() != ContactRequestStatus.PROCESSING) {
            throw new ConflictException("Chỉ có thể xử lý yêu cầu đang được tiếp nhận");
        }
        UUID currentStaffId = authService.currentStaffId();
        if (authService.getCurrentSystemRole() != SystemRole.CLINIC_MANAGER
                && (entity.getAssignedStaff() == null
                || !entity.getAssignedStaff().getStaffId().equals(currentStaffId))) {
            throw new ConflictException("Yêu cầu đang được nhân viên khác phụ trách");
        }
        entity.setInternalNote(request.internalNote().trim());
        entity.setStatus(targetStatus);
        entity.setCompletedAt(LocalDateTime.now());
        return ContactRequestResponse.from(repository.save(entity));
    }

    private ContactRequest findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu liên hệ không tồn tại"));
    }

    private StaffInfo currentStaff() {
        UUID staffId = authService.currentStaffId();
        if (staffId == null) throw new BadRequestException("Không xác định được nhân viên hiện tại");
        return staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên hiện tại không tồn tại"));
    }

    private String generateCode() {
        return "LH-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
