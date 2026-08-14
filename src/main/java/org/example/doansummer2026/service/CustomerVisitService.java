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
import org.example.doansummer2026.model.Invoice;
import org.example.doansummer2026.enums.VisitStatus;
import org.example.doansummer2026.repository.AppointmentRepository;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.example.doansummer2026.repository.InsuranceRuleRepository;
import org.example.doansummer2026.repository.InvoiceRepository;
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
    private final InvoiceRepository invoiceRepo;

    @Transactional(readOnly = true)
    public PageResponse<CustomerVisitResponse> search(UUID customerId, VisitStatus status,
                                                      LocalDateTime from, LocalDateTime to,
                                                      Pageable pageable) {
        Page<CustomerVisit> page = repo.search(customerId, status, from, to, pageable);
        return PageResponse.from(page, this::toResponse);
    }

    @Transactional(readOnly = true)
    public CustomerVisitResponse get(UUID id) {
        return toResponse(findById(id));
    }

    public CustomerVisitResponse create(CustomerVisitCreateRequest req) {
        if (req.serviceIds() == null || req.serviceIds().isEmpty()) {
            throw new org.example.doansummer2026.exception.BadRequestException("Vui lòng chọn ít nhất một dịch vụ khám");
        }

        Profile customer;
        if (req.customerId() != null) {
            customer = profileRepo.findById(req.customerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Khách hàng không tồn tại: " + req.customerId()));
        } else {
            String guestPhone = req.guestPhone() == null ? null : req.guestPhone().trim();
            // Bệnh nhân từng khám có thể là hồ sơ khách vãng lai chưa có account.
            // Tái sử dụng hồ sơ theo SĐT thay vì tạo Profile trùng lặp.
            customer = guestPhone == null || guestPhone.isBlank()
                    ? null
                    : profileRepo.findFirstByPhone(guestPhone).orElse(null);
            if (customer == null) {
                customer = Profile.builder()
                        .fullName(req.guestFullName())
                        .phone(guestPhone)
                        .address(req.guestAddress())
                        .dateOfBirth(req.guestDateOfBirth())
                        .gender(req.guestGender() != null ? req.guestGender() : org.example.doansummer2026.enums.Gender.OTHER)
                        .build();
                customer = profileRepo.save(customer);
            }
        }
        customer = profileRepo.findByIdForUpdate(customer.getProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ bệnh nhân"));
        UUID customerIdForCheck = customer.getProfileId();
        repo.findFirstByCustomer_ProfileIdAndStatusInOrderByCheckInTimeDesc(
                customerIdForCheck, List.of(VisitStatus.CHECKED_IN, VisitStatus.IN_PROGRESS))
                .ifPresent(active -> {
                    String code = "VIS-" + active.getVisitId().toString().substring(0, 8).toUpperCase();
                    throw new ConflictException("Bệnh nhân đang có lượt khám " + code
                            + " chưa hoàn thành. Không thể tạo thêm lượt khám đồng thời");
                });
        Appointment appointment = null;
        if (req.appointmentId() != null) {
            appointment = appointmentRepo.findById(req.appointmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Lịch hẹn không tồn tại: " + req.appointmentId()));
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
                        .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại: " + serviceId));
                        
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
        findById(id);
        throw new ConflictException("Không thể xóa lượt khám vì đây là dữ liệu nghiệp vụ. Hãy hủy lượt trước khi phát sinh thanh toán hoặc khám");
    }

    public CustomerVisit findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lượt khám không tồn tại: " + id));
    }

    private CustomerVisitResponse toResponse(CustomerVisit visit) {
        Invoice invoice = invoiceRepo.findAllByVisit_VisitId(visit.getVisitId()).stream()
                .max(java.util.Comparator.comparing(Invoice::getCreatedAt))
                .orElse(null);
        return CustomerVisitResponse.from(visit, invoice);
    }
}



