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
import org.example.doansummer2026.model.Account;
import org.example.doansummer2026.model.Appointment;
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.model.Insurance;
import org.example.doansummer2026.repository.AccountRepository;
import org.example.doansummer2026.repository.AppointmentRepository;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.example.doansummer2026.repository.InsuranceRepository;
import org.example.doansummer2026.repository.InsuranceRuleRepository;
import org.example.doansummer2026.service.interfaces.AppointmentServiceInterface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
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

        Appointment a = Appointment.builder()
                .customer(customer)
                .scheduledAt(req.scheduledAt())
                .cancelReason(req.cancelReason())
                .timeSlot(req.timeSlot())
                .status(AppointmentStatus.PENDING)
                .build();
        if (req.serviceIds() != null && !req.serviceIds().isEmpty()) {
            Set<MedicalService> services = new HashSet<>();
            for (UUID serviceId : req.serviceIds()) {
                MedicalService service = serviceRepo.findById(serviceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Dich vu khong ton tai: " + serviceId));
                services.add(service);
            }
            a.setServices(services);
        }
        return AppointmentResponse.from(repo.save(a));
    }

    public AppointmentResponse createForGuest(AppointmentGuestCreateRequest req) {
        Appointment a = Appointment.builder()
                .scheduledAt(req.scheduledAt())
                .isGuest(true)
                .guestFullName(req.guestFullName())
                .guestPhone(req.guestPhone())
                .guestAddress(req.guestAddress())
                .guestAge(req.guestAge())
                .guestGender(req.guestGender())
                .timeSlot(req.timeSlot())
                .status(AppointmentStatus.PENDING)
                .build();
        if (req.serviceIds() != null && !req.serviceIds().isEmpty()) {
            Set<MedicalService> services = new HashSet<>();
            for (UUID serviceId : req.serviceIds()) {
                MedicalService service = serviceRepo.findById(serviceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Dich vu khong ton tai: " + serviceId));
                services.add(service);
            }
            a.setServices(services);
        }
        return AppointmentResponse.from(repo.save(a));
    }

    public AppointmentResponse update(UUID id, AppointmentUpdateRequest req) {
        Appointment a = findById(id);
        if (req.scheduledAt() != null) a.setScheduledAt(req.scheduledAt());
        if (req.status() != null) a.setStatus(req.status());
        if (req.cancelReason() != null) a.setCancelReason(req.cancelReason());
        if (req.timeSlot() != null) a.setTimeSlot(req.timeSlot());
        
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
        return AppointmentResponse.from(repo.save(a));
    }

    public void delete(UUID id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Lich hen khong ton tai: " + id);
        }
        repo.deleteById(id);
    }

    /**
     * Check-in tu appointment: tao CustomerVisit + Invoice cho dich vu dau tien.
     * - QueueTicket se duoc tao khi Invoice duoc thanh toan (trong InvoiceService).
     * - serviceIds (optional): Cho phep thay doi dich vu khi check-in.
     */
    public AppointmentCheckInResponse checkIn(AppointmentCheckInRequest req) {
        Appointment a = findById(req.appointmentId());

        // Kiem tra da check-in chua
        if (a.getStatus() == AppointmentStatus.CHECKED_IN) {
            throw new BadRequestException("Appointment da duoc check-in: " + req.appointmentId());
        }

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

        // Tao hoac lay CustomerVisit hien co
        CustomerVisit visit = visitRepo.findByAppointment_AppointmentId(a.getAppointmentId())
            .orElseGet(() -> {
                CustomerVisit newVisit;
                if (Boolean.TRUE.equals(a.getIsGuest())) {
                    newVisit = CustomerVisit.builder()
                            .appointment(a)
                            .checkInTime(LocalDateTime.now())
                            .status(VisitStatus.CHECKED_IN)
                            .build();
                } else {
                    newVisit = CustomerVisit.builder()
                            .customer(a.getCustomer())
                            .appointment(a)
                            .checkInTime(LocalDateTime.now())
                            .status(VisitStatus.CHECKED_IN)
                            .build();
                }
                return visitRepo.save(newVisit);
            });
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
        CustomerVisit visit = CustomerVisit.builder()
                .checkInTime(LocalDateTime.now())
                .status(VisitStatus.CHECKED_IN)
                .build();
        CustomerVisit savedVisit = visitRepo.save(visit);

        // Tao invoice rong cho guest check-in
        var invoiceResponse = invoiceService.create(new org.example.doansummer2026.dto.invoice.InvoiceCreateRequest(
                null,
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

        Appointment a = repo.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Lich hen khong ton tai"));

        if (a.getCustomer() == null || !a.getCustomer().getProfileId().equals(customer.getProfileId())) {
            throw new BadRequestException("Khong co quyen truy cap lich hen nay");
        }

        if (a.getStatus() != AppointmentStatus.PENDING) {
            throw new BadRequestException("Chi co the cap nhat lich hen khi chua check-in");
        }

        if (req.scheduledAt() != null) a.setScheduledAt(req.scheduledAt());
        if (req.timeSlot() != null) a.setTimeSlot(req.timeSlot());
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

        Appointment appointment = repo.findById(appointmentId)
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



