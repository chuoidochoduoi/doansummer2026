package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.appointment.AppointmentCheckInRequest;
import org.example.doansummer2026.dto.appointment.AppointmentCheckInResponse;
import org.example.doansummer2026.dto.appointment.GuestCheckInRequest;
import org.example.doansummer2026.dto.appointment.GuestCheckInResponse;
import org.example.doansummer2026.dto.appointment.AppointmentCreateRequest;
import org.example.doansummer2026.dto.appointment.AppointmentGuestCreateRequest;
import org.example.doansummer2026.dto.appointment.AppointmentResponse;
import org.example.doansummer2026.dto.appointment.AppointmentUpdateRequest;
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
import org.example.doansummer2026.repository.AccountRepository;
import org.example.doansummer2026.repository.AppointmentRepository;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.example.doansummer2026.service.interfaces.AppointmentServiceInterface;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
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
        if (account.getRole() != Role.PATIENT && account.getRole() != Role.RECEPTIONIST) {
            throw new BadRequestException("Only PATIENT and RECEPTIONIST can book appointments");
        }

        // Tim profile tu account
        Profile customer = profileRepo.findByAccount_AccountId(req.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile khong ton tai cho tai khoan nay"));

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


        // Tao CustomerVisit
        CustomerVisit visit;
        if (Boolean.TRUE.equals(a.getIsGuest())) {
            visit = CustomerVisit.builder()
                    .appointment(a)
                    .checkInTime(LocalDateTime.now())
                    .status(VisitStatus.CHECKED_IN)
                    .build();
        } else {
            visit = CustomerVisit.builder()
                    .customer(a.getCustomer())
                    .appointment(a)
                    .checkInTime(LocalDateTime.now())
                    .status(VisitStatus.CHECKED_IN)
                    .build();
        }
        CustomerVisit savedVisit = visitRepo.save(visit);

        // Tao Invoice DRAFT thay vi QueueTicket
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
                null
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
     * Check-in truc tiep cho khach vang lai (khong co appointment).
     * - Tao CustomerVisit + Invoice, QueueTicket se duoc tao khi thanh toan.
     */
    public GuestCheckInResponse guestCheckIn(GuestCheckInRequest req) {
        // Validate services
        Set<MedicalService> services = new HashSet<>();
        for (UUID serviceId : req.serviceIds()) {
            MedicalService service = serviceRepo.findById(serviceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Dich vu khong ton tai: " + serviceId));
            services.add(service);
        }


        CustomerVisit visit = CustomerVisit.builder()
                .checkInTime(LocalDateTime.now())
                .status(VisitStatus.CHECKED_IN)
                .build();
        CustomerVisit savedVisit = visitRepo.save(visit);

        // Tao Invoice DRAFT thay vi QueueTicket
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
}