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
import org.example.doansummer2026.enums.AppointmentStatus;
import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.enums.ServiceStatus;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.repository.AppointmentRepository;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.example.doansummer2026.repository.InsuranceRuleRepository;
import org.example.doansummer2026.repository.InvoiceRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
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
import java.time.Period;
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
    private final StaffInfoRepository staffInfoRepository;

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
        validateSingleExaminationService(req.serviceIds());
        if (req.insuranceId() != null) {
            throw new BadRequestException("Bảo hiểm y tế chỉ được xác nhận và áp dụng tại quầy thu ngân");
        }

        Profile customer;
        if (req.customerId() != null) {
            customer = profileRepo.findById(req.customerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Khách hàng không tồn tại: " + req.customerId()));
        } else {
            String guestPhone = req.guestPhone() == null ? null : req.guestPhone().trim();
            validateGuestInformation(req, guestPhone);
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
                        .gender(req.guestGender())
                        .build();
                customer = profileRepo.save(customer);
            }
        }
        customer = profileRepo.findByIdForUpdate(customer.getProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ bệnh nhân"));
        if (req.customerId() == null) {
            // Le tan duoc cap nhat thong tin hanh chinh khi tiep nhan, ke ca khi
            // so dien thoai da tim thay ho so khach vang lai cu.
            customer.setFullName(req.guestFullName().trim().replaceAll("\\s+", " "));
            customer.setAddress(req.guestAddress() == null || req.guestAddress().isBlank()
                    ? null : req.guestAddress().trim());
            if (req.guestDateOfBirth() != null) customer.setDateOfBirth(req.guestDateOfBirth());
            customer.setGender(req.guestGender());
            profileRepo.save(customer);
        }
        if (customer.getGender() == null || customer.getGender() == Gender.OTHER) {
            throw new BadRequestException("Vui lòng cập nhật giới tính Nam hoặc Nữ trước khi tạo lượt khám");
        }
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
            appointment = appointmentRepo.findByIdForUpdate(req.appointmentId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Lịch hẹn không tồn tại: " + req.appointmentId()));
            validateAppointmentForCheckIn(appointment, customer);
        }

        org.example.doansummer2026.model.StaffInfo checkedInBy = staffInfo(req.issuedById());
        CustomerVisit v = CustomerVisit.builder()
                .customer(customer)
                .appointment(appointment)
                .checkInTime(LocalDateTime.now())
                .checkedInBy(checkedInBy)
                .status(VisitStatus.CHECKED_IN)
                .build();
        CustomerVisit saved = repo.save(v);
        if (appointment != null) {
            appointment.setStatus(org.example.doansummer2026.enums.AppointmentStatus.CHECKED_IN);
            appointmentRepo.save(appointment);
        }

        // Tao InvoiceItem cho moi service (gia mac dinh tu MedicalService) - optional
        List<org.example.doansummer2026.dto.invoice.InvoiceItemCreateRequest> items = new ArrayList<>();
        List<UUID> serviceIds = req.serviceIds().stream().distinct().toList();
        BigDecimal totalDiscount = BigDecimal.ZERO;
        
        if (serviceIds != null && !serviceIds.isEmpty()) {
            for (UUID serviceId : serviceIds) {
                MedicalService service = serviceRepo.findById(serviceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại: " + serviceId));
                validateServiceEligibility(service, customer);

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
        CustomerVisit v = repo.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lượt khám không tồn tại: " + id));
        if (req.checkOutTime() != null) {
            throw new BadRequestException("Không được tự nhập thời gian kết thúc lượt khám");
        }
        if (req.status() != VisitStatus.CANCELLED) {
            throw new ConflictException(
                    "Trạng thái lượt khám phải được cập nhật qua đúng thao tác hàng chờ và hoàn thành hồ sơ");
        }
        if (v.getStatus() != VisitStatus.CHECKED_IN) {
            throw new ConflictException("Chỉ có thể hủy lượt khám đang chờ tiếp nhận");
        }
        for (Invoice invoice : invoiceRepo.findAllByVisit_VisitId(id)) {
            invoiceService.cancel(invoice.getInvoiceId());
        }
        v.setStatus(VisitStatus.CANCELLED);
        v.setCheckOutTime(LocalDateTime.now());
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

    private void validateSingleExaminationService(List<UUID> serviceIds) {
        long examinationCount = serviceIds.stream().distinct()
                .map(serviceId -> serviceRepo.findById(serviceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại: " + serviceId)))
                .filter(service -> service.getDepartmentType() != null
                        && service.getDepartmentType().normalized()
                        == org.example.doansummer2026.enums.DepartmentType.EXAMINATION)
                .count();
        if (examinationCount > 1) {
            throw new org.example.doansummer2026.exception.BadRequestException(
                    "Mỗi lượt khám chỉ được chọn một dịch vụ khám bệnh. Bạn vẫn có thể chọn nhiều dịch vụ cận lâm sàng"
            );
        }
    }

    private void validateGuestInformation(CustomerVisitCreateRequest req, String phone) {
        String fullName = req.guestFullName() == null ? "" : req.guestFullName().trim();
        if (fullName.length() < 2) {
            throw new BadRequestException("Vui lòng nhập họ tên bệnh nhân");
        }
        if (fullName.codePoints().anyMatch(Character::isDigit)) {
            throw new BadRequestException("Họ tên không được chứa chữ số");
        }
        if (phone == null || !phone.matches("^(\\+84|0)\\d{9,10}$")) {
            throw new BadRequestException("Số điện thoại Việt Nam không hợp lệ");
        }
        if (req.guestGender() == null || req.guestGender() == Gender.OTHER) {
            throw new BadRequestException("Hệ thống chỉ hỗ trợ giới tính Nam hoặc Nữ");
        }
    }

    private void validateAppointmentForCheckIn(Appointment appointment, Profile customer) {
        if (appointment.getStatus() != AppointmentStatus.PENDING
                && appointment.getStatus() != AppointmentStatus.RESCHEDULED) {
            throw new ConflictException("Lịch hẹn không còn ở trạng thái chờ tiếp nhận");
        }
        if (repo.findByAppointment_AppointmentId(appointment.getAppointmentId()).isPresent()) {
            throw new ConflictException("Lịch hẹn đã được check-in trước đó");
        }
        if (appointment.getScheduledAt() == null
                || !appointment.getScheduledAt().toLocalDate().equals(clinicToday())) {
            throw new BadRequestException("Chỉ có thể check-in lịch hẹn đúng ngày khám");
        }
        if (appointment.getCustomer() != null
                && !appointment.getCustomer().getProfileId().equals(customer.getProfileId())) {
            throw new ConflictException("Lịch hẹn không thuộc bệnh nhân đã chọn");
        }
        if (appointment.getCustomer() == null && appointment.getGuestPhone() != null
                && !normalizePhone(appointment.getGuestPhone()).equals(normalizePhone(customer.getPhone()))) {
            throw new ConflictException("Số điện thoại bệnh nhân không khớp với lịch hẹn");
        }
    }

    private void validateServiceEligibility(MedicalService service, Profile customer) {
        if (service.getStatus() != ServiceStatus.ACTIVE) {
            throw new BadRequestException("Dịch vụ " + service.getName() + " hiện không áp dụng");
        }
        Integer age = customer.getDateOfBirth() == null ? null
                : Period.between(customer.getDateOfBirth(), clinicToday()).getYears();
        if (age == null && (service.getMinimumAge() != null || service.getMaximumAge() != null)) {
            throw new BadRequestException("Vui lòng cập nhật ngày sinh trước khi chọn dịch vụ: " + service.getName());
        }
        if (age != null && service.getMinimumAge() != null && age < service.getMinimumAge()) {
            throw new BadRequestException("Dịch vụ " + service.getName() + " chỉ áp dụng từ "
                    + service.getMinimumAge() + " tuổi");
        }
        if (age != null && service.getMaximumAge() != null && age > service.getMaximumAge()) {
            throw new BadRequestException("Dịch vụ " + service.getName() + " chỉ áp dụng đến "
                    + service.getMaximumAge() + " tuổi");
        }
        if (service.getAllowedGender() != null
                && service.getAllowedGender() != customer.getGender()) {
            throw new BadRequestException("Dịch vụ " + service.getName()
                    + " không phù hợp với giới tính của bệnh nhân");
        }
    }

    private org.example.doansummer2026.model.StaffInfo staffInfo(UUID staffId) {
        if (staffId == null) {
            throw new BadRequestException("Không tìm thấy nhân viên đang tiếp nhận bệnh nhân");
        }
        return staffInfoRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên tiếp nhận"));
    }

    private LocalDate clinicToday() {
        return LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("\\D", "");
        return digits.startsWith("84") ? "0" + digits.substring(2) : digits;
    }
}



