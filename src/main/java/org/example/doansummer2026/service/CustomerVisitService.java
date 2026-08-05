package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.customerVisit.CustomerVisitCreateRequest;
import org.example.doansummer2026.dto.customerVisit.CustomerVisitResponse;
import org.example.doansummer2026.dto.customerVisit.CustomerVisitUpdateRequest;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.model.Appointment;
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.enums.VisitStatus;
import org.example.doansummer2026.repository.AppointmentRepository;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.example.doansummer2026.repository.InsuranceRuleRepository;
import org.example.doansummer2026.model.InsuranceRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.CustomerVisitServiceInterface;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final InsuranceRuleRepository insuranceRuleRepo;

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
        if (req.serviceIds() == null || req.serviceIds().isEmpty()) {
            throw new org.example.doansummer2026.exception.BadRequestException("Vui long chon it nhat 1 dich vu kham");
        }

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
        customer = profileRepo.findByIdForUpdate(customer.getProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay ho so benh nhan"));
        UUID customerIdForCheck = customer.getProfileId();
        repo.findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                customerIdForCheck, List.of(VisitStatus.CHECKED_IN, VisitStatus.IN_PROGRESS))
                .ifPresent(active -> {
                    String code = "VIS-" + active.getVisitId().toString().substring(0, 8).toUpperCase();
                    throw new ConflictException("Benh nhan dang co luot kham " + code
                            + " chua hoan thanh. Khong the tao them luot kham dong thoi");
                });
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
        BigDecimal totalDiscount = BigDecimal.ZERO;
        
        if (serviceIds != null && !serviceIds.isEmpty()) {
            for (UUID serviceId : serviceIds) {
                MedicalService service = serviceRepo.findById(serviceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Dich vu khong ton tai: " + serviceId));
                        
                BigDecimal unitPrice = service.getPrice();
                BigDecimal discountPercent = BigDecimal.ZERO;
                
                // Fetch insurance rule if insuranceId is provided
                if (req.insuranceId() != null) {
                    var ruleOpt = insuranceRuleRepo.findByInsurance_InsuranceIdAndDepartmentType(
                            req.insuranceId(), service.getDepartmentType());
                    if (ruleOpt.isPresent()) {
                        discountPercent = ruleOpt.get().getDiscountPercent();
                    }
                }
                
                BigDecimal discountAmount = unitPrice.multiply(discountPercent).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                BigDecimal finalPrice = unitPrice.subtract(discountAmount);
                totalDiscount = totalDiscount.add(discountAmount);
                
                items.add(new org.example.doansummer2026.dto.invoice.InvoiceItemCreateRequest(
                        serviceId,
                        service.getName(),
                        service.getServiceCode(),
                        unitPrice,
                        1,
                        discountPercent,
                        discountAmount,
                        finalPrice,
                        null
                ));
            }
        }

        var invoiceResponse = invoiceService.create(new org.example.doansummer2026.dto.invoice.InvoiceCreateRequest(
                customer.getProfileId(),
                saved.getVisitId(),
                null,
                null,
                totalDiscount,
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



