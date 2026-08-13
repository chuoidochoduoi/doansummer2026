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
import org.example.doansummer2026.enums.Gender;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AppointmentService implements AppointmentServiceInterface {

    private static final long APPOINTMENT_CONFLICT_MINUTES = 30;

    private final AppointmentRepository repo;
    private final ProfileRepository profileRepo;
    private final AccountRepository accountRepo;
    private final CustomerVisitRepository visitRepo;
    private final MedicalServiceRepository serviceRepo;
    private final InvoiceService invoiceService;
    private final StaffInfoRepository staffRepo;
    private final NotificationService notificationService;
    private final ShiftConfigRepository shiftConfigRepository;

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
                .orElseThrow(() -> new ResourceNotFoundException("Tai khoan khong ton tai: " + req.customerId()));

        // Kiem tra role co the dat lich kham
        if (account.getRole() != Role.CUSTOMER && account.getRole() != Role.STAFF) {
            throw new BadRequestException("Only CUSTOMER and STAFF can book appointments");
        }

        Profile customer = profileRepo.findFirstByAccount_AccountId(req.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Benh nhan khong ton tai (Customer ID)"));

        // Cho phep nhieu lich trong tuong lai, chi chan cac lich bi chong thoi gian.
        if (hasAppointmentConflict(customer.getProfileId(), req.scheduledAt())) {
            throw new BadRequestException("Bạn đã có lịch hẹn khác trùng hoặc quá gần thời gian này");
        }

        org.example.doansummer2026.model.ShiftConfig shift = req.shiftId() != null 
                ? shiftConfigRepository.findById(req.shiftId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ca kham khong ton tai"))
                : null;

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
                        .orElseThrow(() -> new ResourceNotFoundException("Dich vu khong ton tai: " + serviceId));
                Integer age = customer.getDateOfBirth() != null
                        ? Period.between(customer.getDateOfBirth(), req.scheduledAt().toLocalDate()).getYears() : null;
                validateServiceEligibility(service, age, customer.getGender());
                services.add(service);
            }
            a.setServices(services);
        }
        Appointment saved = repo.save(a);
        notifyReceptionists(saved);
        return AppointmentResponse.from(saved);
    }

    private boolean hasAppointmentConflict(UUID customerId, LocalDateTime scheduledAt) {
        return repo.existsCustomerConflict(
                customerId,
                List.of(AppointmentStatus.PENDING),
                scheduledAt.minusMinutes(APPOINTMENT_CONFLICT_MINUTES),
                scheduledAt.plusMinutes(APPOINTMENT_CONFLICT_MINUTES + 1));
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public AppointmentResponse createForGuest(AppointmentGuestCreateRequest req) {
        if (req.guestGender() == Gender.OTHER) {
            throw new BadRequestException("He thong chi ho tro gioi tinh MALE hoac FEMALE");
        }
        org.example.doansummer2026.model.ShiftConfig shift = req.shiftId() != null 
                ? shiftConfigRepository.findById(req.shiftId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ca kham khong ton tai"))
                : null;

        LocalDateTime conflictFrom = req.scheduledAt().minusMinutes(APPOINTMENT_CONFLICT_MINUTES);
        LocalDateTime conflictTo = req.scheduledAt().plusMinutes(APPOINTMENT_CONFLICT_MINUTES + 1);
        if ((emptyToNull(req.guestPhone()) != null || emptyToNull(req.guestEmail()) != null)
                && repo.existsGuestConflict(
                        emptyToNull(req.guestPhone()),
                        emptyToNull(req.guestEmail()),
                        List.of(AppointmentStatus.PENDING),
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
                        .orElseThrow(() -> new ResourceNotFoundException("Dich vu khong ton tai: " + serviceId));
                validateServiceEligibility(service, req.guestAge(), req.guestGender());
                services.add(service);
            }
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
            validateRescheduleConflict(a, req.scheduledAt());
            a.setScheduledAt(req.scheduledAt());
        }
        if (req.status() != null) a.setStatus(req.status());
        if (req.cancelReason() != null) a.setCancelReason(req.cancelReason());
        if (req.shiftId() != null) {
            org.example.doansummer2026.model.ShiftConfig shift = shiftConfigRepository.findById(req.shiftId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ca kham khong ton tai"));
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
                        .orElseThrow(() -> new ResourceNotFoundException("Dich vu khong ton tai: " + serviceId));
                services.add(service);
            }
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
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Lich hen khong ton tai: " + id);
        }
        repo.deleteById(id);
    }

    private void validateServiceEligibility(MedicalService service, Integer age, Gender gender) {
        if (age == null && (service.getMinimumAge() != null || service.getMaximumAge() != null)) {
            throw new BadRequestException("Vui long cap nhat ngay sinh truoc khi dat dich vu: " + service.getName());
        }
        if (age != null && service.getMinimumAge() != null && age < service.getMinimumAge()) {
            throw new BadRequestException("Dich vu " + service.getName() + " chi ap dung tu " + service.getMinimumAge() + " tuoi");
        }
        if (age != null && service.getMaximumAge() != null && age > service.getMaximumAge()) {
            throw new BadRequestException("Dich vu " + service.getName() + " chi ap dung den " + service.getMaximumAge() + " tuoi");
        }
        if (service.getAllowedGender() != null) {
            if (gender == null) {
                throw new BadRequestException("Vui long cap nhat gioi tinh truoc khi dat dich vu: " + service.getName());
            }
            if (service.getAllowedGender() != gender) {
                throw new BadRequestException("Dich vu " + service.getName() + " khong phu hop voi gioi tinh trong ho so");
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
                .orElseThrow(() -> new ResourceNotFoundException("Lich hen khong ton tai: " + req.appointmentId()));

        // Kiem tra da check-in chua
        if (a.getStatus() == AppointmentStatus.CHECKED_IN) {
            throw new ConflictException("Lich hen da duoc nhan vien khac check-in");
        }
        if (a.getStatus() != AppointmentStatus.PENDING) {
            throw new BadRequestException("Chi co the check-in lich hen dang cho tiep nhan");
        }
        
        if (!a.getScheduledAt().toLocalDate().equals(LocalDate.now())) {
            throw new BadRequestException("Chỉ có thể check-in lịch hẹn đúng ngày (hôm nay). Lịch hẹn này vào ngày " + a.getScheduledAt().toLocalDate());
        }

        if (a.getScheduledAt() != null && !a.getScheduledAt().toLocalDate().equals(java.time.LocalDate.now())) {
            throw new BadRequestException("Chỉ có thể check-in đúng ngày đã hẹn");
        }

        if (req.issuedById() == null) {
            throw new BadRequestException("Khong tim thay nhan vien le tan dang thuc hien check-in");
        }
        StaffInfo checkedInBy = staffRepo.findById(req.issuedById())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nhan vien thuc hien check-in"));

        updatePatientInformation(a, req.patientFullName(), req.patientPhone(), req.patientEmail(),
                req.patientAddress(), req.patientDateOfBirth(), req.patientAge(), req.patientGender());

        // Thay doi dich vu neu duoc cung cap, hoac lay services hien co
        Set<MedicalService> services;
        if (req.serviceIds() != null && !req.serviceIds().isEmpty()) {
            services = new HashSet<>();
            for (UUID serviceId : req.serviceIds()) {
                MedicalService service = serviceRepo.findById(serviceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Dich vu khong ton tai: " + serviceId));
                services.add(service);
            }
            a.setServices(services);
        } else {
            services = a.getServices();
            if (services == null || services.isEmpty()) {
                throw new BadRequestException("Appointment chua chon dich vu");
            }
        }

        repo.save(a); // Luu lai appointment voi services moi (neu co)


        // Khoa ho so benh nhan trong transaction de hai nhan vien khong the
        // dong thoi tao hai luot kham dang hoat dong.
        Profile visitCustomer;
        if (Boolean.TRUE.equals(a.getIsGuest())) {
            visitCustomer = profileRepo.findFirstByPhone(a.getGuestPhone()).orElseGet(() ->
                    profileRepo.save(Profile.builder()
                            .fullName(a.getGuestFullName()).phone(a.getGuestPhone())
                            .email(a.getGuestEmail()).address(a.getGuestAddress())
                            .dateOfBirth(req.patientDateOfBirth()).gender(a.getGuestGender()).build()));
        } else {
            visitCustomer = a.getCustomer();
        }
        Profile lockedCustomer = profileRepo.findByIdForUpdate(visitCustomer.getProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay ho so benh nhan"));
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
                        throw new ConflictException("So dien thoai da duoc su dung");
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
                        throw new ConflictException("Email da duoc su dung");
                    }
                });
            }
            customer.setEmail(normalizedEmail);
        }

        profileRepo.save(customer);
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
                .orElseThrow(() -> new ResourceNotFoundException("Lich hen khong ton tai: " + id));
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
        Profile guestProfile = profileRepo.findFirstByPhone(req.guestPhone()).orElseGet(() ->
                profileRepo.save(Profile.builder()
                        .fullName(req.guestFullName()).phone(req.guestPhone()).build()));
        guestProfile = profileRepo.findByIdForUpdate(guestProfile.getProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay ho so benh nhan"));
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
        var invoiceItems = req.serviceIds() == null ? java.util.List.<org.example.doansummer2026.dto.invoice.InvoiceItemCreateRequest>of()
                : req.serviceIds().stream()
                .map(serviceId -> serviceRepo.findById(serviceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Dich vu khong ton tai: " + serviceId)))
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
                    throw new ConflictException("Benh nhan dang co luot kham " + code
                            + " chua hoan thanh. Vui long hoan thanh hoac huy luot hien tai truoc khi check-in lich moi");
                });
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerAppointmentResponse> getMyAppointments(UUID customerId, String code, String specialty, String status, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        // Tim profile tu account
        Profile customer = profileRepo.findFirstByAccount_AccountId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Benh nhan khong ton tai"));

        Page<Appointment> page = repo.searchForCustomer(customer.getProfileId(), code, specialty, status, from, to, pageable);
        return PageResponse.from(page, CustomerAppointmentResponse::from);
    }

    @Transactional(readOnly = true)
    public CustomerAppointmentDetailResponse getMyAppointmentDetail(UUID customerId, UUID appointmentId) {
        Profile customer = profileRepo.findFirstByAccount_AccountId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Benh nhan khong ton tai"));

        Appointment appointment = repo.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Lich hen khong ton tai"));

        if (appointment.getCustomer() == null || !appointment.getCustomer().getProfileId().equals(customer.getProfileId())) {
            throw new BadRequestException("Khong co quyen truy cap lich hen nay");
        }

        return CustomerAppointmentDetailResponse.from(appointment);
    }

    public CustomerAppointmentDetailResponse updateMyAppointment(UUID customerId, UUID appointmentId, AppointmentUpdateRequest req) {
        Profile customer = profileRepo.findFirstByAccount_AccountId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Benh nhan khong ton tai"));

        Appointment a = repo.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Lich hen khong ton tai"));

        if (a.getCustomer() == null || !a.getCustomer().getProfileId().equals(customer.getProfileId())) {
            throw new BadRequestException("Khong co quyen truy cap lich hen nay");
        }

        if (a.getStatus() != AppointmentStatus.PENDING) {
            throw new BadRequestException("Chi co the cap nhat lich hen khi chua check-in");
        }

        if (req.scheduledAt() != null) {
            validateRescheduleConflict(a, req.scheduledAt());
            a.setScheduledAt(req.scheduledAt());
        }
        if (req.shiftId() != null) {
            org.example.doansummer2026.model.ShiftConfig shift = shiftConfigRepository.findById(req.shiftId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ca kham khong ton tai"));
            a.setShiftName(shift.getName());
            a.setShiftTime(shift.getStartTime() + " - " + shift.getEndTime());
        }
        if (req.serviceIds() != null && !req.serviceIds().isEmpty()) {
            Set<MedicalService> services = new HashSet<>();
            for (UUID serviceId : req.serviceIds()) {
                MedicalService service = serviceRepo.findById(serviceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Dich vu khong ton tai: " + serviceId));
                services.add(service);
            }
            a.setServices(services);
        }
        return CustomerAppointmentDetailResponse.from(repo.save(a));
    }

    private void validateRescheduleConflict(Appointment appointment, LocalDateTime scheduledAt) {
        if (appointment.getCustomer() == null) return;
        boolean conflict = repo.existsOtherCustomerConflict(
                appointment.getCustomer().getProfileId(),
                appointment.getAppointmentId(),
                List.of(AppointmentStatus.PENDING),
                scheduledAt.minusMinutes(APPOINTMENT_CONFLICT_MINUTES),
                scheduledAt.plusMinutes(APPOINTMENT_CONFLICT_MINUTES + 1));
        if (conflict) {
            throw new BadRequestException("Bạn đã có lịch hẹn khác trùng hoặc quá gần thời gian này");
        }
    }

    public void cancelMyAppointment(UUID customerId, UUID appointmentId) {
        Profile customer = profileRepo.findFirstByAccount_AccountId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Benh nhan khong ton tai"));

        Appointment appointment = repo.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Lich hen khong ton tai"));

        if (appointment.getCustomer() == null || !appointment.getCustomer().getProfileId().equals(customer.getProfileId())) {
            throw new BadRequestException("Khong co quyen truy cap lich hen nay");
        }

        if (appointment.getStatus() != AppointmentStatus.PENDING) {
            throw new BadRequestException("Chi co the huy lich hen dang cho xac nhan hoac chua check-in");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        repo.save(appointment);
    }
}
