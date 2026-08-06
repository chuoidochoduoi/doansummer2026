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
import org.example.doansummer2026.model.Insurance;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.repository.AccountRepository;
import org.example.doansummer2026.repository.AppointmentRepository;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.example.doansummer2026.repository.InsuranceRepository;
import org.example.doansummer2026.repository.InsuranceRuleRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.example.doansummer2026.repository.ShiftConfigRepository;
import org.example.doansummer2026.service.interfaces.AppointmentServiceInterface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    private final AppointmentRepository repo;
    private final ProfileRepository profileRepo;
    private final AccountRepository accountRepo;
    private final CustomerVisitRepository visitRepo;
    private final MedicalServiceRepository serviceRepo;
    private final InvoiceService invoiceService;
    private final InsuranceRepository insuranceRepository;
    private final InsuranceRuleRepository insuranceRuleRepository;
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

        // Kiem tra da co cuoc hen nao dang hoat dong chua (1 customer 1 appointment at a time)
        boolean hasActive = repo.existsByCustomer_ProfileIdAndStatusIn(customer.getProfileId(), 
                java.util.List.of(AppointmentStatus.PENDING));
        if (hasActive) {
            throw new BadRequestException("Bạn đã có hẹn nhé");
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

    public AppointmentResponse createForGuest(AppointmentGuestCreateRequest req) {
        if (req.guestGender() == Gender.OTHER) {
            throw new BadRequestException("He thong chi ho tro gioi tinh MALE hoac FEMALE");
        }
        org.example.doansummer2026.model.ShiftConfig shift = req.shiftId() != null 
                ? shiftConfigRepository.findById(req.shiftId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ca kham khong ton tai"))
                : null;

        Appointment a = Appointment.builder()
                .scheduledAt(req.scheduledAt())
                .isGuest(true)
                .guestFullName(req.guestFullName())
                .guestPhone(req.guestPhone())
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
        
        if (req.scheduledAt() != null) a.setScheduledAt(req.scheduledAt());
        if (req.status() != null) a.setStatus(req.status());
        if (req.cancelReason() != null) a.setCancelReason(req.cancelReason());
        if (req.shiftId() != null) {
            org.example.doansummer2026.model.ShiftConfig shift = shiftConfigRepository.findById(req.shiftId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ca kham khong ton tai"));
            a.setShiftName(shift.getName());
            a.setShiftTime(shift.getStartTime() + " - " + shift.getEndTime());
        }
        
        // Cập nhật thông tin khách (cho phép ghi đè kể cả khách vãng lai hay khách có tk)
        if (req.guestFullName() != null) a.setGuestFullName(req.guestFullName());
        if (req.guestPhone() != null) a.setGuestPhone(req.guestPhone());
        if (req.guestAddress() != null) a.setGuestAddress(req.guestAddress());
        if (req.guestAge() != null) a.setGuestAge(req.guestAge());
        if (req.guestGender() != null) a.setGuestGender(req.guestGender());

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
        String content = String.format("Co lich hen moi tu %s vao luc %s", patientName, a.getScheduledAt());
        
        List<StaffInfo> receptionists = staffRepo.findAllBySystemRoleIn(List.of(SystemRole.RECEPTIONIST));
        for (StaffInfo staff : receptionists) {
            if (staff.getProfile() != null) {
                try {
                    notificationService.create(new NotificationCreateRequest(
                            staff.getProfile().getProfileId(),
                            NotificationType.GENERAL,
                            NotificationChannel.IN_APP,
                            "Lich hen moi",
                            content,
                            "Appointment",
                            a.getAppointmentId()
                    ));
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
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
        if (req.issuedById() == null) {
            throw new BadRequestException("Khong tim thay nhan vien le tan dang thuc hien check-in");
        }
        StaffInfo checkedInBy = staffRepo.findById(req.issuedById())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay nhan vien thuc hien check-in"));

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


        // Lay bao hiem neu co
        Insurance insurance = null;
        List<org.example.doansummer2026.model.InsuranceRule> insuranceRules = List.of();
        if (req.insuranceId() != null) {
            insurance = insuranceRepository.findById(req.insuranceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bao hiem khong ton tai: " + req.insuranceId()));
            insuranceRules = insuranceRuleRepository.findByInsurance_InsuranceId(req.insuranceId());
        }

        // Khoa ho so benh nhan trong transaction de hai nhan vien khong the
        // dong thoi tao hai luot kham dang hoat dong.
        Profile visitCustomer;
        if (Boolean.TRUE.equals(a.getIsGuest())) {
            visitCustomer = profileRepo.findFirstByPhone(a.getGuestPhone()).orElseGet(() ->
                    profileRepo.save(Profile.builder()
                            .fullName(a.getGuestFullName()).phone(a.getGuestPhone())
                            .address(a.getGuestAddress()).gender(a.getGuestGender()).build()));
        } else {
            visitCustomer = a.getCustomer();
        }
        Profile lockedCustomer = profileRepo.findByIdForUpdate(visitCustomer.getProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay ho so benh nhan"));
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

        final List<org.example.doansummer2026.model.InsuranceRule> finalRules = insuranceRules;

        var invoiceItems = services.stream()
                .map(s -> {
                    BigDecimal price = s.getPrice() != null ? s.getPrice() : BigDecimal.ZERO;
                    BigDecimal discountPercent = BigDecimal.ZERO;
                    
                    if (!finalRules.isEmpty() && s.getDepartment() != null) {
                        for (org.example.doansummer2026.model.InsuranceRule rule : finalRules) {
                            if (rule.getDepartmentType() == s.getDepartment().getDepartmentType()) {
                                discountPercent = rule.getDiscountPercent();
                                break;
                            }
                        }
                    }
                    
                    BigDecimal discountAmount = price.multiply(discountPercent).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
                    BigDecimal finalPrice = price.subtract(discountAmount);
                    
                    return new org.example.doansummer2026.dto.invoice.InvoiceItemCreateRequest(
                            s.getServiceId(),
                            s.getName(),
                            s.getServiceCode(),
                            price,
                            1,
                            discountPercent,
                            discountAmount,
                            finalPrice,
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

        // Tao invoice rong cho guest check-in
        var invoiceResponse = invoiceService.create(new org.example.doansummer2026.dto.invoice.InvoiceCreateRequest(
                guestProfile.getProfileId(),
                savedVisit.getVisitId(),
                null,
                null,
                null,
                null,
                null,
                req.issuedById(),
                null
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
    public PageResponse<CustomerAppointmentResponse> getMyAppointments(UUID customerId, String code, String specialty, String status, Pageable pageable) {
        // Tim profile tu account
        Profile customer = profileRepo.findFirstByAccount_AccountId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Benh nhan khong ton tai"));

        Page<Appointment> page = repo.searchForCustomer(customer.getProfileId(), code, specialty, status, pageable);
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

        if (req.scheduledAt() != null) a.setScheduledAt(req.scheduledAt());
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
