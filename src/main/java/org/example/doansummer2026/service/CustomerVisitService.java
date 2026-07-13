package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.customerVisit.CustomerVisitCreateRequest;
import org.example.doansummer2026.dto.customerVisit.CustomerVisitResponse;
import org.example.doansummer2026.dto.customerVisit.CustomerVisitUpdateRequest;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Appointment;
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.enums.VisitStatus;
import org.example.doansummer2026.repository.AppointmentRepository;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.CustomerVisitServiceInterface;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CustomerVisitService implements CustomerVisitServiceInterface {

    private final CustomerVisitRepository repo;
    private final ProfileRepository profileRepo;
    private final AppointmentRepository appointmentRepo;
    private final MedicalServiceRepository serviceRepo;
    private final InvoiceService invoiceService;

    @Transactional(readOnly = true)
    public PageResponse<CustomerVisitResponse> search(UUID customerId, VisitStatus status,
                                                      LocalDateTime from, LocalDateTime to,
                                                      Pageable pageable) {
        Page<CustomerVisit> page = repo.search(customerId, status, from, to, pageable);
        return PageResponse.from(page, CustomerVisitResponse::from);
    }

    @Transactional(readOnly = true)
    public CustomerVisitResponse get(UUID id) {
        return CustomerVisitResponse.from(findById(id));
    }

    public CustomerVisitResponse create(CustomerVisitCreateRequest req) {
        Profile customer;
        if (req.customerId() != null) {
            customer = profileRepo.findById(req.customerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Khach hang khong ton tai: " + req.customerId()));
        } else {
            // Tao Profile moi cho khach vang lai
            customer = Profile.builder()
                    .fullName(req.guestFullName())
                    .phone(req.guestPhone())
                    .address(req.guestAddress())
                    .dateOfBirth(req.guestDateOfBirth())
                    .gender(req.guestGender() != null ? req.guestGender() : org.example.doansummer2026.enums.Gender.OTHER)
                    .build();
            customer = profileRepo.save(customer);
        }
        Appointment appointment = null;
        if (req.appointmentId() != null) {
            appointment = appointmentRepo.findById(req.appointmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Lich hen khong ton tai: " + req.appointmentId()));
        }

        CustomerVisit v = CustomerVisit.builder()
                .customer(customer)
                .appointment(appointment)
                .checkInTime(LocalDateTime.now())
                .status(VisitStatus.CHECKED_IN)
                .build();
        CustomerVisit saved = repo.save(v);
        if (appointment != null) {
            appointment.setStatus(org.example.doansummer2026.enums.AppointmentStatus.CHECKED_IN);
            appointmentRepo.save(appointment);
        }

        // Tao InvoiceItem cho moi service (gia mac dinh tu MedicalService) - optional
        List<org.example.doansummer2026.dto.invoice.InvoiceItemCreateRequest> items = new ArrayList<>();
        List<UUID> serviceIds = req.serviceIds();
        if (serviceIds != null && !serviceIds.isEmpty()) {
            for (UUID serviceId : serviceIds) {
                MedicalService service = serviceRepo.findById(serviceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Dich vu khong ton tai: " + serviceId));
                items.add(new org.example.doansummer2026.dto.invoice.InvoiceItemCreateRequest(
                        serviceId,
                        service.getName(),
                        null, // serviceCodeSnapshot
                        service.getPrice(),
                        1,
                        null
                ));
            }
        }

        // Tao Invoice PENDING khi tao CustomerVisit (co items)
        var invoiceResponse = invoiceService.create(new org.example.doansummer2026.dto.invoice.InvoiceCreateRequest(
                customer.getProfileId(),
                saved.getVisitId(),
                null,
                null,
                null,
                null,
                null,
                req.issuedById(),
                items.isEmpty() ? null : items
        ));
        return CustomerVisitResponse.from(saved, invoiceResponse.invoiceId());
    }

    public CustomerVisitResponse update(UUID id, CustomerVisitUpdateRequest req) {
        CustomerVisit v = findById(id);
        if (req.status() != null) v.setStatus(req.status());
        if (req.checkOutTime() != null) v.setCheckOutTime(req.checkOutTime());
        if (req.status() == VisitStatus.COMPLETED && v.getCheckOutTime() == null) {
            v.setCheckOutTime(LocalDateTime.now());
        }
        return CustomerVisitResponse.from(repo.save(v));
    }

    public void delete(UUID id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Luot kham khong ton tai: " + id);
        }
        repo.deleteById(id);
    }

    public CustomerVisit findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Luot kham khong ton tai: " + id));
    }
}