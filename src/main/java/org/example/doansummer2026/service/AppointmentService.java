package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.appointment.AppointmentCheckInRequest;
import org.example.doansummer2026.dto.appointment.AppointmentCheckInResponse;
import org.example.doansummer2026.dto.appointment.GuestCheckInRequest;
import org.example.doansummer2026.dto.appointment.GuestCheckInResponse;
import org.example.doansummer2026.dto.appointment.GuestHistoryResponse;
import org.example.doansummer2026.dto.appointment.AppointmentCreateRequest;
import org.example.doansummer2026.dto.appointment.AppointmentGuestCreateRequest;
import org.example.doansummer2026.dto.appointment.AppointmentResponse;
import org.example.doansummer2026.dto.appointment.AppointmentUpdateRequest;
import org.example.doansummer2026.dto.appointment.CustomerAppointmentResponse;
import org.example.doansummer2026.dto.appointment.CustomerAppointmentDetailResponse;
import org.example.doansummer2026.enums.AppointmentStatus;
import org.example.doansummer2026.enums.Role;
import org.example.doansummer2026.enums.VisitStatus;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.dto.notification.NotificationCreateRequest;
import org.example.doansummer2026.enums.NotificationType;
import org.example.doansummer2026.enums.NotificationChannel;
import org.example.doansummer2026.enums.SystemRole;
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.model.Appointment;
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.repository.AccountRepository;
import org.example.doansummer2026.repository.AppointmentRepository;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.example.doansummer2026.repository.InvoiceRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.example.doansummer2026.repository.ShiftConfigRepository;
import org.example.doansummer2026.service.interfaces.AppointmentServiceInterface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.time.Period;
import java.time.ZoneId;
import org.example.doansummer2026.enums.Gender;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AppointmentService implements AppointmentServiceInterface {

    private static final long APPOINTMENT_CONFLICT_MINUTES = 30;
    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final AppointmentRepository repo;
    private final ProfileRepository profileRepo;
    private final AccountRepository accountRepo;
    private final CustomerVisitRepository visitRepo;
    private final MedicalServiceRepository serviceRepo;
    private final InvoiceService invoiceService;
    private final StaffInfoRepository staffRepo;
    private final NotificationService notificationService;
    private final ShiftConfigRepository shiftConfigRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponse> search(UUID customerId,
                                                         AppointmentStatus status,
                                                         LocalDateTime from, LocalDateTime to,
                                                         Pageable pageable) {
        Page<Appointment> page = repo.search(customerId, status == null ? null : status.name(), from, to, pageable);
        return PageResponse.from(page, AppointmentResponse::from);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse get(UUID id) {
        return AppointmentResponse.from(findById(id));
    }

    public AppointmentResponse create(AppointmentCreateRequest req) {
        // Tim account theo accountId
        Account account = accountRepo.findById(req.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại: " + req.customerId()));

        // Kiem tra role co the dat lich kham
        if (account.getRole() != Role.CUSTOMER && account.getRole() != Role.STAFF) {
            throw new BadRequestException("Chỉ khách hàng hoặc nhân viên mới có thể đặt lịch hẹn");
        }

        Profile customer = profileRepo.findFirstByAccount_AccountId(req.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Bệnh nhân không tồn tại"));

        // Cho phep nhieu lich trong tuong lai, chi chan cac lich bi chong thoi gian.
        if (hasAppointmentConflict(customer.getProfileId(), req.scheduledAt())) {
            throw new BadRequestException("Bạn đã có lịch hẹn khác trùng hoặc quá gần thời gian này");
        }

        org.example.doansummer2026.model.ShiftConfig shift = req.shiftId() != null 
                ? shiftConfigRepository.findById(req.shiftId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ca khám không tồn tại"))
                : null;
        validateAppointmentTiming(req.scheduledAt(), shift);

        Appointment a = Appointment.builder()
                .customer(customer)
                .scheduledAt(req.scheduledAt())
                .cancelReason(req.cancelReason())
                .shiftName(shift != null ? shift.getName() : null)
                .shiftTime(shift != null ? shift.getStartTime() + " - " + shift.getEndTime() : null)
                .status(AppointmentStatus.PENDING)
                .build();
        if (req.serviceIds() != null && !req.serviceIds().isEmpty()) {
            Set<MedicalService> services = new HashSet<>();
            for (UUID serviceId : req.serviceIds()) {
                MedicalService service = serviceRepo.findById(serviceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại: " + serviceId));
                Integer age = customer.getDateOfBirth() != null
                        ? Period.between(customer.getDateOfBirth(), req.scheduledAt().toLocalDate()).getYears() : null;
                validateServiceEligibility(service, age, customer.getGender());
                services.add(service);
            }
            validateSingleExaminationService(services);
            a.setServices(services);
        }
        Appointment saved = repo.save(a);
        notifyReceptionists(saved);
        return AppointmentResponse.from(saved);
    }

    private boolean hasAppointmentConflict(UUID customerId, LocalDateTime scheduledAt) {
        return repo.existsCustomerConflict(
                customerId,
                activeAppointmentStatuses(),
                scheduledAt.minusMinutes(APPOINTMENT_CONFLICT_MINUTES),
                scheduledAt.plusMinutes(APPOINTMENT_CONFLICT_MINUTES + 1));
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public AppointmentResponse createForGuest(AppointmentGuestCreateRequest req) {
        if (req.guestGender() == Gender.OTHER) {
            throw new BadRequestException("Hệ thống chỉ hỗ trợ giới tính Nam hoặc Nữ");
        }
        org.example.doansummer2026.model.ShiftConfig shift = req.shiftId() != null 
                ? shiftConfigRepository.findById(req.shiftId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ca khám không tồn tại"))
                : null;
        validateAppointmentTiming(req.scheduledAt(), shift);

        LocalDateTime conflictFrom = req.scheduledAt().minusMinutes(APPOINTMENT_CONFLICT_MINUTES);
        LocalDateTime conflictTo = req.scheduledAt().plusMinutes(APPOINTMENT_CONFLICT_MINUTES + 1);
        if ((emptyToNull(req.guestPhone()) != null || emptyToNull(req.guestEmail()) != null)
                && repo.existsGuestConflict(
                        emptyToNull(req.guestPhone()),
                        emptyToNull(req.guestEmail()),
                        activeAppointmentStatuses(),
                        conflictFrom,
                        conflictTo)) {
            throw new BadRequestException("Khách hàng đã có lịch hẹn khác trùng hoặc quá gần thời gian này");
        }

        Appointment a = Appointment.builder()
                .scheduledAt(req.scheduledAt())
                .isGuest(true)
                .guestFullName(req.guestFullName())
                .guestPhone(req.guestPhone())
                .guestEmail(req.guestEmail())
                .guestAddress(req.guestAddress())
                .guestAge(req.guestAge())
                .guestGender(req.guestGender())
                .shiftName(shift != null ? shift.getName() : null)
                .shiftTime(shift != null ? shift.getStartTime() + " - " + shift.getEndTime() : null)
                .status(AppointmentStatus.PENDING)
                .build();
        if (req.serviceIds() != null && !req.serviceIds().isEmpty()) {
            Set<MedicalService> services = new HashSet<>();
            for (UUID serviceId : req.serviceIds()) {
                MedicalService service = serviceRepo.findById(serviceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại: " + serviceId));
                validateServiceEligibility(service, req.guestAge(), req.guestGender());
                services.add(service);
            }
            validateSingleExaminationService(services);
            a.setServices(services);
        }
        Appointment saved = repo.save(a);
        notifyReceptionists(saved);
        return AppointmentResponse.from(saved);
    }

    public AppointmentResponse update(UUID id, AppointmentUpdateRequest req) {
        Appointment a = findById(id);
        AppointmentStatus oldStatus = a.getStatus();

        if (oldStatus == AppointmentStatus.CANCELLED || oldStatus == AppointmentStatus.CHECKED_IN) {
            throw new BadRequestException("Không thể sửa lịch hẹn đã Hủy hoặc đã Check-In");
        }

        if (req.scheduledAt() != null) {
            validateAppointmentTiming(req.scheduledAt(), resolveRequestedShift(req.shiftId(), a));
            validateRescheduleConflict(a, req.scheduledAt());
            a.setScheduledAt(req.scheduledAt());
        }
        validateStaffStatusUpdate(oldStatus, req);
        if (req.status() != null) a.setStatus(req.status());
        if (req.cancelReason() != null) a.setCancelReason(req.cancelReason());
        if (req.shiftId() != null) {
            org.example.doansummer2026.model.ShiftConfig shift = shiftConfigRepository.findById(req.shiftId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ca khám không tồn tại"));
            a.setShiftName(shift.getName());
            a.setShiftTime(shift.getStartTime() + " - " + shift.getEndTime());
        }

        // Cập nhật thông tin khách (cho phép ghi đè kể cả khách vãng lai hay khách có tk)
        updatePatientInformation(a, req.guestFullName(), req.guestPhone(), req.guestEmail(),
                req.guestAddress(), req.guestDateOfBirth(), req.guestAge(), req.guestGender());

        if (req.serviceIds() != null && !req.serviceIds().isEmpty()) {
            Set<MedicalService> services = new HashSet<>();
            for (UUID serviceId : req.serviceIds()) {
                MedicalService service = serviceRepo.findById(serviceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại: " + serviceId));
                Integer age = resolveAppointmentAge(a, req.guestDateOfBirth(), req.guestAge());
                Gender gender = req.guestGender() != null ? req.guestGender()
                        : (a.getCustomer() != null ? a.getCustomer().getGender() : a.getGuestGender());
                validateServiceEligibility(service, age, gender);
                services.add(service);
            }
            validateSingleExaminationService(services);
            a.setServices(services);
        }

        Appointment saved = repo.save(a);
        if (req.status() != null && req.status() != oldStatus) {
            notifyCustomerStatusChange(saved, oldStatus);
            if (req.status() == AppointmentStatus.CANCELLED) {
                String patientName = (saved.getIsGuest() != null && saved.getIsGuest()) ? saved.getGuestFullName() : (saved.getCustomer() != null ? saved.getCustomer().getFullName() : "Khách");
                notificationService.notifyStaffByRole(SystemRole.RECEPTIONIST, "Lịch hẹn đã bị hủy", String.format("%s đã hủy lịch hẹn vào lúc %s", patientName, saved.getScheduledAt()), "Appointment", saved.getAppointmentId());
            }
        }

        return AppointmentResponse.from(saved);
    }

    public void delete(UUID id) {
        findById(id);
        throw new ConflictException("Không xóa lịch hẹn để tránh mất lịch sử. Vui lòng dùng chức năng hủy lịch hẹn");
    }

    private void validateServiceEligibility(MedicalService service, Integer age, Gender gender) {
        if (service.getStatus() != org.example.doansummer2026.enums.ServiceStatus.ACTIVE) {
            throw new BadRequestException("Dịch vụ " + service.getName() + " hiện không áp dụng");
        }
        if (age == null && (service.getMinimumAge() != null || service.getMaximumAge() != null)) {
            throw new BadRequestException("Vui lòng cập nhật ngày sinh trước khi đặt dịch vụ: " + service.getName());
        }
        if (age != null && service.getMinimumAge() != null && age < service.getMinimumAge()) {
            throw new BadRequestException("Dịch vụ " + service.getName() + " chỉ áp dụng từ " + service.getMinimumAge() + " tuổi");
        }
        if (age != null && service.getMaximumAge() != null && age > service.getMaximumAge()) {
            throw new BadRequestException("Dịch vụ " + service.getName() + " chỉ áp dụng đến " + service.getMaximumAge() + " tuổi");
        }
        if (service.getAllowedGender() != null) {
            if (gender == null) {
                throw new BadRequestException("Vui lòng cập nhật giới tính trước khi đặt dịch vụ: " + service.getName());
            }
            if (service.getAllowedGender() != gender) {
                throw new BadRequestException("Dịch vụ " + service.getName() + " không phù hợp với giới tính trong hồ sơ");
            }
        }
    }

    private void notifyReceptionists(Appointment a) {
        String patientName = (a.getIsGuest() != null && a.getIsGuest()) ? a.getGuestFullName() : (a.getCustomer() != null ? a.getCustomer().getFullName() : "Khach");
        String content = String.format("Có lịch hẹn mới từ %s vào lúc %s", patientName, a.getScheduledAt());
        notificationService.notifyStaffByRole(SystemRole.RECEPTIONIST, "Lịch hẹn mới", content, "Appointment", a.getAppointmentId());
    }

    private void notifyCustomerStatusChange(Appointment a, AppointmentStatus oldStatus) {
        if ((a.getIsGuest() != null && a.getIsGuest()) || a.getCustomer() == null) return;
        if (a.getStatus() == AppointmentStatus.CHECKED_IN || a.getStatus() == AppointmentStatus.CANCELLED || a.getStatus() == AppointmentStatus.RESCHEDULED) {
            String statusStr = a.getStatus() == AppointmentStatus.CHECKED_IN ? "da duoc tiep nhan" : (a.getStatus() == AppointmentStatus.CANCELLED ? "bi huy" : "duoc doi lich");
            String content = String.format("Lich hen cua ban vao luc %s %s", a.getScheduledAt(), statusStr);
            try {
                notificationService.create(new NotificationCreateRequest(
                        a.getCustomer().getProfileId(),
                        NotificationType.GENERAL,
                        NotificationChannel.IN_APP,
                        "Cap nhat lich hen",
                        content,
                        "Appointment",
                        a.getAppointmentId()
                    ));
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    /**
     * Check-in tu appointment: tao CustomerVisit + Invoice cho dich vu dau tien.
     * - QueueTicket se duoc tao khi Invoice duoc thanh toan (trong InvoiceService).
     * - serviceIds (optional): Cho phep thay doi dich vu khi check-in.
     */
    public AppointmentCheckInResponse checkIn(AppointmentCheckInRequest req) {
        Appointment a = repo.findByIdForUpdate(req.appointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Lịch hẹn không tồn tại: " + req.appointmentId()));

        // Kiem tra da check-in chua
        if (a.getStatus() == AppointmentStatus.CHECKED_IN) {
            throw new ConflictException("Lịch hẹn đã được nhân viên khác check-in");
        }
        if (!isAwaitingCheckIn(a.getStatus())) {
            throw new BadRequestException("Chỉ có thể check-in lịch hẹn đang chờ tiếp nhận");
        }
        
        if (!a.getScheduledAt().toLocalDate().equals(clinicToday())) {
            throw new BadRequestException("Chỉ có thể check-in lịch hẹn đúng ngày (hôm nay). Lịch hẹn này vào ngày " + a.getScheduledAt().toLocalDate());
        }

        if (req.issuedById() == null) {
            throw new BadRequestException("Không tìm thấy nhân viên lễ tân đang thực hiện check-in");
        }
        StaffInfo checkedInBy = staffRepo.findById(req.issuedById())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên thực hiện check-in"));

        String previousPhone = a.getCustomer() != null ? a.getCustomer().getPhone() : a.getGuestPhone();
        String previousEmail = a.getCustomer() != null ? a.getCustomer().getEmail() : a.getGuestEmail();
        Profile profileBeforeContactChange = resolveExistingPatientProfile(a, previousPhone, previousEmail);
        if (profileBeforeContactChange != null) {
            linkGuestHistoryBeforeContactChange(profileBeforeContactChange, previousPhone, previousEmail);
        }

        updatePatientInformation(a, req.patientFullName(), req.patientPhone(), req.patientEmail(),
                req.patientAddress(), req.patientDateOfBirth(), req.patientAge(), req.patientGender());

        // Thay doi dich vu neu duoc cung cap, hoac lay services hien co
        Set<MedicalService> services;
        if (req.serviceIds() != null && !req.serviceIds().isEmpty()) {
            services = new HashSet<>();
            for (UUID serviceId : req.serviceIds()) {
                MedicalService service = serviceRepo.findById(serviceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại: " + serviceId));
                Integer age = req.patientDateOfBirth() != null
                        ? Period.between(req.patientDateOfBirth(), clinicToday()).getYears()
                        : (a.getCustomer() != null && a.getCustomer().getDateOfBirth() != null
                        ? Period.between(a.getCustomer().getDateOfBirth(), clinicToday()).getYears()
                        : req.patientAge());
                Gender gender = req.patientGender() != null ? req.patientGender()
                        : (a.getCustomer() != null ? a.getCustomer().getGender() : a.getGuestGender());
                validateServiceEligibility(service, age, gender);
                services.add(service);
            }
            validateSingleExaminationService(services);
            a.setServices(services);
        } else {
            services = a.getServices();
            if (services == null || services.isEmpty()) {
                throw new BadRequestException("Lịch hẹn chưa chọn dịch vụ");
            }
        }

        repo.save(a); // Luu lai appointment voi services moi (neu co)


        // Khoa ho so benh nhan trong transaction de hai nhan vien khong the
        // dong thoi tao hai luot kham dang hoat dong.
        Profile visitCustomer;
        if (Boolean.TRUE.equals(a.getIsGuest())) {
            visitCustomer = profileBeforeContactChange != null
                    ? profileBeforeContactChange
                    : profileRepo.findFirstByPhone(a.getGuestPhone()).orElseGet(() ->
                    profileRepo.save(Profile.builder()
                            .fullName(a.getGuestFullName()).phone(a.getGuestPhone())
                            .email(a.getGuestEmail()).address(a.getGuestAddress())
                            .dateOfBirth(req.patientDateOfBirth()).gender(a.getGuestGender()).build()));
        } else {
            visitCustomer = a.getCustomer();
        }
        Profile lockedCustomer = profileRepo.findByIdForUpdate(visitCustomer.getProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ bệnh nhân"));
        if (appointmentHasNoRegisteredCustomer(a)) {
            updateProfileInformation(lockedCustomer, req.patientFullName(), req.patientPhone(),
                    req.patientEmail(), req.patientAddress(), req.patientDateOfBirth(),
                    req.patientGender());
        }
        ensureNoActiveVisit(lockedCustomer.getProfileId());

        CustomerVisit visit = visitRepo.findByAppointment_AppointmentId(a.getAppointmentId())
                .orElseGet(() -> visitRepo.save(CustomerVisit.builder()
                        .customer(lockedCustomer)
                        .appointment(a)
                        .checkInTime(LocalDateTime.now())
                        .checkedInBy(checkedInBy)
                        .status(VisitStatus.CHECKED_IN)
                        .build()));
        CustomerVisit savedVisit = visit;

        var invoiceItems = services.stream()
                .map(s -> {
                    BigDecimal price = s.getPrice() != null ? s.getPrice() : BigDecimal.ZERO;
                    
                    return new org.example.doansummer2026.dto.invoice.InvoiceItemCreateRequest(
                            s.getServiceId(),
                            s.getName(),
                            s.getServiceCode(),
                            price,
                            1,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            price,
                            null
                    );
                })
                .toList();

        // Tao Invoice
        UUID customerId = savedVisit.getCustomer() != null ? savedVisit.getCustomer().getProfileId() : null;
        var invoiceResponse = invoiceService.create(new org.example.doansummer2026.dto.invoice.InvoiceCreateRequest(
                customerId,
                savedVisit.getVisitId(),
                null,
                null,
                null,
                null,
                null,
                req.issuedById(),
                invoiceItems // Them items vao
        ));

        // Cap nhat appointment status
        a.setStatus(AppointmentStatus.CHECKED_IN);
        repo.save(a);

        return AppointmentCheckInResponse.from(a, savedVisit, invoiceResponse.invoiceId());
    }

    private void updatePatientInformation(
            Appointment appointment,
            String fullName,
            String phone,
            String email,
            String address,
            LocalDate dateOfBirth,
            Integer age,
            Gender gender
    ) {
        String normalizedPhone = normalizeOptional(phone);
        String normalizedEmail = normalizeOptional(email);
        if (normalizedEmail != null) normalizedEmail = normalizedEmail.toLowerCase();

        if (fullName != null) appointment.setGuestFullName(fullName.trim());
        if (phone != null) appointment.setGuestPhone(normalizedPhone);
        if (email != null) appointment.setGuestEmail(normalizedEmail);
        if (address != null) appointment.setGuestAddress(normalizeOptional(address));
        if (age != null) appointment.setGuestAge(age);
        if (gender != null) appointment.setGuestGender(gender);

        Profile customer = appointment.getCustomer();
        if (customer == null) return;
        updateProfileInformation(customer, fullName, phone, email, address, dateOfBirth, gender);
    }

    private void updateProfileInformation(
            Profile customer,
            String fullName,
            String phone,
            String email,
            String address,
            LocalDate dateOfBirth,
            Gender gender
    ) {
        if (fullName != null && fullName.codePoints().anyMatch(Character::isDigit)) {
            throw new BadRequestException("Họ tên không được chứa chữ số");
        }
        String normalizedPhone = normalizeOptional(phone);
        String normalizedEmail = normalizeOptional(email);
        if (normalizedEmail != null) normalizedEmail = normalizedEmail.toLowerCase();

        if (fullName != null) customer.setFullName(fullName.trim());
        if (dateOfBirth != null) customer.setDateOfBirth(dateOfBirth);
        if (gender != null) customer.setGender(gender);
        if (address != null) customer.setAddress(normalizeOptional(address));

        if (phone != null && !java.util.Objects.equals(normalizedPhone, customer.getPhone())) {
            if (normalizedPhone != null) {
                profileRepo.findFirstByPhone(normalizedPhone).ifPresent(existing -> {
                    if (!existing.getProfileId().equals(customer.getProfileId())) {
                        throw new ConflictException("Số điện thoại đã được sử dụng");
                    }
                });
            }
            customer.setPhone(normalizedPhone);
        }

        if (email != null && !java.util.Objects.equals(normalizedEmail, customer.getEmail())) {
            if (normalizedEmail != null) {
                String finalEmail = normalizedEmail;
                profileRepo.findFirstByEmail(finalEmail).ifPresent(existing -> {
                    if (!existing.getProfileId().equals(customer.getProfileId())) {
                        throw new ConflictException("Email đã được sử dụng");
                    }
                });
            }
            customer.setEmail(normalizedEmail);
        }

        profileRepo.save(customer);
    }

    private Profile resolveExistingPatientProfile(Appointment appointment, String phone, String email) {
        if (appointment.getCustomer() != null) return appointment.getCustomer();
        if (phone != null && !phone.isBlank()) {
            Profile byPhone = profileRepo.findFirstByPhoneIn(phoneVariants(phone)).orElse(null);
            if (byPhone != null) return byPhone;
        }
        if (email != null && !email.isBlank()) {
            return profileRepo.findFirstByEmailIgnoreCase(email).orElse(null);
        }
        return null;
    }

    /**
     * Nối dữ liệu khách vãng lai theo thông tin liên hệ cũ vào cùng một hồ sơ
     * trước khi lễ tân thay số điện thoại/email. Các lượt khám và hóa đơn vì thế
     * tiếp tục được truy vấn bằng profileId thay vì phụ thuộc vào số cũ.
     */
    private void linkGuestHistoryBeforeContactChange(Profile profile, String oldPhone, String oldEmail) {
        Set<String> phones = phoneVariants(oldPhone);
        Set<String> emails = contactValues(oldEmail == null ? null : oldEmail.toLowerCase());
        if (phones.isEmpty() && emails.isEmpty()) return;

        List<Appointment> guestAppointments = repo.findGuestAppointmentsByPhonesOrEmails(phones, emails);
        for (Appointment guestAppointment : guestAppointments) {
            guestAppointment.setCustomer(profile);
            if (profile.getAccount() != null) {
                guestAppointment.setIsGuest(false);
            }
            repo.save(guestAppointment);

            visitRepo.findByAppointment_AppointmentId(guestAppointment.getAppointmentId()).ifPresent(visit -> {
                visit.setCustomer(profile);
                visitRepo.save(visit);
                var invoices = invoiceRepository.findAllByVisit_VisitId(visit.getVisitId());
                invoices.forEach(invoice -> invoice.setCustomer(profile));
                invoiceRepository.saveAll(invoices);
            });
        }
    }

    private Set<String> contactValues(String value) {
        if (value == null || value.isBlank()) return Set.of();
        return Set.of(value.trim());
    }

    private Set<String> phoneVariants(String phone) {
        if (phone == null || phone.isBlank()) return Set.of();
        String original = phone.trim();
        String digits = original.replaceAll("\\D", "");
        String normalized = digits.startsWith("84") && digits.length() >= 11
                ? "0" + digits.substring(2)
                : digits;
        Set<String> values = new java.util.LinkedHashSet<>();
        values.add(original);
        if (!normalized.isBlank()) values.add(normalized);
        if (normalized.startsWith("0") && normalized.length() > 1) {
            values.add("84" + normalized.substring(1));
            values.add("+84" + normalized.substring(1));
        }
        return values;
    }

    private boolean appointmentHasNoRegisteredCustomer(Appointment appointment) {
        return appointment.getCustomer() == null;
    }

    private String normalizeOptional(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }


    public Appointment findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lịch hẹn không tồn tại: " + id));
    }

    /**
     * Kiem tra guest da tung den kham chua (theo phone).
     * Tra ve danh sach cac appointment gan day cua guest.
     */
    @Transactional(readOnly = true)
    public List<GuestHistoryResponse> getGuestHistoryByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return List.of();
        }
        return repo.findGuestAppointmentsByPhone(phone).stream()
                .map(GuestHistoryResponse::from)
                .toList();
    }

    /**
     * Check-in truc tiep cho khach vang lai (khong co appointment).
     * - Tao CustomerVisit + Invoice, QueueTicket se duoc tao khi thanh toan.
     * - serviceIds (optional): Neu null hoac rong se tao invoice rong.
     */
    public GuestCheckInResponse guestCheckIn(GuestCheckInRequest req) {
        if (req.issuedById() == null) {
            throw new BadRequestException("Không tìm thấy nhân viên lễ tân đang thực hiện check-in");
        }
        if (req.serviceIds() == null || req.serviceIds().isEmpty()) {
            throw new BadRequestException("Vui lòng chọn ít nhất một dịch vụ");
        }
        Set<MedicalService> selectedServices = req.serviceIds().stream()
                .map(serviceId -> serviceRepo.findById(serviceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại: " + serviceId)))
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        validateSingleExaminationService(selectedServices);
        selectedServices.forEach(service -> validateServiceEligibility(
                service, req.guestAge(), req.guestGender()));
        if (req.guestFullName().codePoints().anyMatch(Character::isDigit)) {
            throw new BadRequestException("Họ tên không được chứa chữ số");
        }
        if (req.guestGender() == Gender.OTHER) {
            throw new BadRequestException("Hệ thống chỉ hỗ trợ giới tính Nam hoặc Nữ");
        }

        Profile guestProfile = profileRepo.findFirstByPhone(req.guestPhone()).orElseGet(() ->
                profileRepo.save(Profile.builder()
                        .fullName(req.guestFullName().trim()).phone(req.guestPhone())
                        .address(normalizeOptional(req.guestAddress()))
                        .gender(req.guestGender()).build()));
        guestProfile = profileRepo.findByIdForUpdate(guestProfile.getProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ bệnh nhân"));
        updateProfileInformation(guestProfile, req.guestFullName(), null, null,
                req.guestAddress(), null, req.guestGender());
        ensureNoActiveVisit(guestProfile.getProfileId());
        CustomerVisit visit = CustomerVisit.builder()
                .customer(guestProfile)
                .checkedInBy(req.issuedById()!=null?staffRepo.findById(req.issuedById()).orElse(null):null)
                .checkInTime(LocalDateTime.now())
                .status(VisitStatus.CHECKED_IN)
                .build();
        CustomerVisit savedVisit = visitRepo.save(visit);

        // Khach vang lai van phai co cac InvoiceItem tu dich vu da chon.
        // Truoc day req.serviceIds bi bo qua o nhanh nay, nen hoa don van PAID
        // nhung rong va khong the sinh TestRequest/QueueTicket sau thanh toan.
        var invoiceItems = selectedServices.stream()
                .map(service -> {
                    java.math.BigDecimal price = service.getPrice() != null
                            ? service.getPrice() : java.math.BigDecimal.ZERO;
                    return new org.example.doansummer2026.dto.invoice.InvoiceItemCreateRequest(
                            service.getServiceId(),
                            service.getName(),
                            service.getServiceCode(),
                            price,
                            1,
                            java.math.BigDecimal.ZERO,
                            java.math.BigDecimal.ZERO,
                            price,
                            null
                    );
                })
                .toList();

        var invoiceResponse = invoiceService.create(new org.example.doansummer2026.dto.invoice.InvoiceCreateRequest(
                guestProfile.getProfileId(),
                savedVisit.getVisitId(),
                null,
                null,
                null,
                null,
                null,
                req.issuedById(),
                invoiceItems.isEmpty() ? null : invoiceItems
        ));

        return GuestCheckInResponse.from(savedVisit, invoiceResponse.invoiceId(), req.guestFullName(), req.guestPhone());
    }

    private void ensureNoActiveVisit(UUID profileId) {
        visitRepo.findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                profileId, List.of(VisitStatus.CHECKED_IN, VisitStatus.IN_PROGRESS))
                .ifPresent(active -> {
                    String code = "VIS-" + active.getVisitId().toString().substring(0, 8).toUpperCase();
                    throw new ConflictException("Bệnh nhân đang có lượt khám " + code
                            + " chưa hoàn thành. Vui lòng hoàn thành hoặc hủy lượt hiện tại trước khi check-in lịch mới");
                });
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerAppointmentResponse> getMyAppointments(UUID customerId, String code, String specialty, String status, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        // Tim profile tu account
        Profile customer = profileRepo.findFirstByAccount_AccountId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Bệnh nhân không tồn tại"));

        Page<Appointment> page = repo.searchForCustomer(customer.getProfileId(), code, specialty, status, from, to, pageable);
        return PageResponse.from(page, CustomerAppointmentResponse::from);
    }

    @Transactional(readOnly = true)
    public CustomerAppointmentDetailResponse getMyAppointmentDetail(UUID customerId, UUID appointmentId) {
        Profile customer = profileRepo.findFirstByAccount_AccountId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Bệnh nhân không tồn tại"));

        Appointment appointment = repo.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Lịch hẹn không tồn tại"));

        if (appointment.getCustomer() == null || !appointment.getCustomer().getProfileId().equals(customer.getProfileId())) {
            throw new BadRequestException("Không có quyền truy cập lịch hẹn này");
        }

        return CustomerAppointmentDetailResponse.from(appointment);
    }

    public CustomerAppointmentDetailResponse updateMyAppointment(UUID customerId, UUID appointmentId, AppointmentUpdateRequest req) {
        Profile customer = profileRepo.findFirstByAccount_AccountId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Bệnh nhân không tồn tại"));

        Appointment a = repo.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Lịch hẹn không tồn tại"));

        if (a.getCustomer() == null || !a.getCustomer().getProfileId().equals(customer.getProfileId())) {
            throw new BadRequestException("Không có quyền truy cập lịch hẹn này");
        }

        if (!isAwaitingCheckIn(a.getStatus())) {
            throw new BadRequestException("Chỉ có thể cập nhật lịch hẹn khi chưa check-in");
        }

        if (req.scheduledAt() != null) {
            validateAppointmentTiming(req.scheduledAt(), resolveRequestedShift(req.shiftId(), a));
            validateRescheduleConflict(a, req.scheduledAt());
            a.setScheduledAt(req.scheduledAt());
        }
        if (req.shiftId() != null) {
            org.example.doansummer2026.model.ShiftConfig shift = shiftConfigRepository.findById(req.shiftId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ca khám không tồn tại"));
            a.setShiftName(shift.getName());
            a.setShiftTime(shift.getStartTime() + " - " + shift.getEndTime());
        }
        if (req.serviceIds() != null && !req.serviceIds().isEmpty()) {
            Set<MedicalService> services = new HashSet<>();
            for (UUID serviceId : req.serviceIds()) {
                MedicalService service = serviceRepo.findById(serviceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại: " + serviceId));
                Integer age = customer.getDateOfBirth() == null ? null
                        : Period.between(customer.getDateOfBirth(),
                        req.scheduledAt() != null ? req.scheduledAt().toLocalDate() : a.getScheduledAt().toLocalDate())
                        .getYears();
                validateServiceEligibility(service, age, customer.getGender());
                services.add(service);
            }
            validateSingleExaminationService(services);
            a.setServices(services);
        }
        return CustomerAppointmentDetailResponse.from(repo.save(a));
    }

    private void validateRescheduleConflict(Appointment appointment, LocalDateTime scheduledAt) {
        if (appointment.getCustomer() == null) return;
        boolean conflict = repo.existsOtherCustomerConflict(
                appointment.getCustomer().getProfileId(),
                appointment.getAppointmentId(),
                activeAppointmentStatuses(),
                scheduledAt.minusMinutes(APPOINTMENT_CONFLICT_MINUTES),
                scheduledAt.plusMinutes(APPOINTMENT_CONFLICT_MINUTES + 1));
        if (conflict) {
            throw new BadRequestException("Bạn đã có lịch hẹn khác trùng hoặc quá gần thời gian này");
        }
    }

    private void validateSingleExaminationService(java.util.Collection<MedicalService> services) {
        long examinationCount = services == null ? 0 : services.stream()
                .filter(service -> service.getDepartmentType() != null
                        && service.getDepartmentType().normalized()
                        == org.example.doansummer2026.enums.DepartmentType.EXAMINATION)
                .count();
        if (examinationCount > 1) {
            throw new BadRequestException(
                    "Mỗi lịch hẹn chỉ được chọn một dịch vụ khám bệnh. Bạn vẫn có thể chọn nhiều dịch vụ cận lâm sàng"
            );
        }
    }

    private LocalDate clinicToday() {
        return LocalDate.now(CLINIC_ZONE);
    }

    private org.example.doansummer2026.model.ShiftConfig resolveRequestedShift(
            UUID shiftId, Appointment appointment) {
        if (shiftId == null) return null;
        return shiftConfigRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Ca khám không tồn tại"));
    }

    private void validateAppointmentTiming(LocalDateTime scheduledAt,
                                           org.example.doansummer2026.model.ShiftConfig shift) {
        if (scheduledAt == null) {
            throw new BadRequestException("Vui lòng chọn ngày giờ khám");
        }
        if (!scheduledAt.toLocalDate().isAfter(clinicToday())) {
            throw new BadRequestException("Lịch hẹn phải được đặt từ ngày mai trở đi");
        }
        if (shift != null) {
            if (!Boolean.TRUE.equals(shift.getIsActive())) {
                throw new ConflictException("Ca khám đã ngừng hoạt động");
            }
            java.time.LocalTime start = java.time.LocalTime.parse(shift.getStartTime());
            java.time.LocalTime end = java.time.LocalTime.parse(shift.getEndTime());
            java.time.LocalTime appointmentTime = scheduledAt.toLocalTime();
            if (appointmentTime.isBefore(start) || !appointmentTime.isBefore(end)) {
                throw new BadRequestException("Giờ hẹn không thuộc khung giờ của ca đã chọn");
            }
        }
    }

    private Integer resolveAppointmentAge(Appointment appointment, LocalDate requestedDateOfBirth,
                                          Integer requestedAge) {
        LocalDate dateOfBirth = requestedDateOfBirth != null ? requestedDateOfBirth
                : appointment.getCustomer() != null ? appointment.getCustomer().getDateOfBirth() : null;
        if (dateOfBirth != null) {
            return Period.between(dateOfBirth, appointment.getScheduledAt().toLocalDate()).getYears();
        }
        return requestedAge != null ? requestedAge : appointment.getGuestAge();
    }

    public void cancelMyAppointment(UUID customerId, UUID appointmentId) {
        Profile customer = profileRepo.findFirstByAccount_AccountId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Bệnh nhân không tồn tại"));

        Appointment appointment = repo.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Lịch hẹn không tồn tại"));

        if (appointment.getCustomer() == null || !appointment.getCustomer().getProfileId().equals(customer.getProfileId())) {
            throw new BadRequestException("Không có quyền truy cập lịch hẹn này");
        }

        if (!isAwaitingCheckIn(appointment.getStatus())) {
            throw new BadRequestException("Chỉ có thể hủy lịch hẹn đang chờ xác nhận hoặc chưa check-in");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        repo.save(appointment);
    }

    private List<AppointmentStatus> activeAppointmentStatuses() {
        return List.of(AppointmentStatus.PENDING, AppointmentStatus.RESCHEDULED);
    }

    private boolean isAwaitingCheckIn(AppointmentStatus status) {
        return status == AppointmentStatus.PENDING || status == AppointmentStatus.RESCHEDULED;
    }

    private void validateStaffStatusUpdate(AppointmentStatus currentStatus, AppointmentUpdateRequest req) {
        AppointmentStatus requestedStatus = req.status();
        if (requestedStatus == null || requestedStatus == currentStatus) return;

        if (requestedStatus == AppointmentStatus.CHECKED_IN) {
            throw new BadRequestException("Vui lòng dùng chức năng check-in để tiếp nhận bệnh nhân");
        }
        if (requestedStatus == AppointmentStatus.PENDING) {
            throw new BadRequestException("Không thể đưa lịch hẹn về trạng thái chờ bằng chức năng cập nhật");
        }
        if (requestedStatus == AppointmentStatus.RESCHEDULED && req.scheduledAt() == null) {
            throw new BadRequestException("Vui lòng chọn thời gian mới khi đổi lịch hẹn");
        }
        if (requestedStatus == AppointmentStatus.CANCELLED
                && (req.cancelReason() == null || req.cancelReason().isBlank())) {
            throw new BadRequestException("Vui lòng nhập lý do hủy lịch hẹn");
        }
    }
}
