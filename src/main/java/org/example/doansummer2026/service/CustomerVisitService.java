package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.customervisit.CustomerVisitCreateRequest;
import org.example.doansummer2026.dto.customervisit.CustomerVisitResponse;
import org.example.doansummer2026.dto.customervisit.CustomerVisitUpdateRequest;
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
import org.example.doansummer2026.enums.AllergyStatus;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.repository.AppointmentRepository;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.example.doansummer2026.repository.InsuranceRuleRepository;
import org.example.doansummer2026.repository.InvoiceRepository;
import org.example.doansummer2026.repository.InvoiceItemRepository;
import org.example.doansummer2026.repository.QueueTicketRepository;
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
    private final InvoiceItemRepository invoiceItemRepo;
    private final QueueTicketRepository queueTicketRepo;
    private final StaffInfoRepository staffInfoRepository;
    private final org.example.doansummer2026.repository.ShiftConfigRepository shiftConfigRepository;
    private final ShiftScheduleResolver shiftScheduleResolver;
    private final ServiceAvailabilityService serviceAvailabilityService;
    private final AuditLogService auditLogService;
    private final tools.jackson.databind.ObjectMapper objectMapper;
    private final MedicalServiceSelectionPolicyService serviceSelectionPolicyService;

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
            throw new org.example.doansummer2026.exception.BadRequestException("Vui lòng chọn ít nhất một dịch vụ y tế");
        }
        List<UUID> normalizedServiceIds = serviceSelectionPolicyService != null
                ? serviceSelectionPolicyService.normalizeOrThrow(req.serviceIds()).stream()
                .map(MedicalService::getServiceId).toList()
                : req.serviceIds().stream().distinct().toList();
        validateSelectedServices(normalizedServiceIds);
        if (req.insuranceId() != null) {
            throw new BadRequestException("Bảo hiểm y tế chỉ được xác nhận và áp dụng tại quầy thu ngân");
        }

        Profile customer;
        boolean existingProfile;
        if (req.customerId() != null) {
            customer = profileRepo.findById(req.customerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Khách hàng không tồn tại: " + req.customerId()));
            existingProfile = true;
        } else {
            String guestPhone = normalizeVietnamesePhone(req.guestPhone());
            validateGuestInformation(req, guestPhone);
            String guestEmail = normalizeEmail(req.guestEmail());
            // Bệnh nhân từng khám có thể là hồ sơ khách chưa có account.
            // Tái sử dụng theo liên hệ duy nhất; không suy đoán bằng họ tên/ngày sinh.
            customer = findExistingGuestProfile(guestPhone, guestEmail);
            existingProfile = customer != null;
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
        String oldProfileJson = existingProfile ? profileSnapshot(customer) : null;
        boolean shouldUpdateProfile = !existingProfile || Boolean.TRUE.equals(req.updatePatientProfile());
        if (shouldUpdateProfile) applyPatientIntake(customer, req);
        String newProfileJson = existingProfile ? profileSnapshot(customer) : null;
        boolean profileChanged = existingProfile && !java.util.Objects.equals(oldProfileJson, newProfileJson);
        if (customer.getGender() == null || customer.getGender() == Gender.OTHER) {
            throw new BadRequestException("Vui lòng cập nhật giới tính Nam hoặc Nữ trước khi tạo lượt khám");
        }
        validateNoSameDayExaminationRegistration(customer.getProfileId(), normalizedServiceIds);
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
        List<UUID> serviceIds = normalizedServiceIds;
        BigDecimal totalDiscount = BigDecimal.ZERO;
        
        if (serviceIds != null && !serviceIds.isEmpty()) {
            for (UUID serviceId : serviceIds) {
                MedicalService service = serviceRepo.findById(serviceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại: " + serviceId));
                validateServiceEligibility(service, customer);
                validateCurrentExaminationAvailability(service);

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
        if (profileChanged) {
            scheduleProfileAudit(customer.getProfileId(), checkedInBy, oldProfileJson, newProfileJson);
        }
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

    private void validateSelectedServices(List<UUID> serviceIds) {
        if (serviceIds.size() != serviceIds.stream().distinct().count()) {
            throw new org.example.doansummer2026.exception.BadRequestException(
                    "Không được chọn trùng dịch vụ trong cùng một lượt khám"
            );
        }
        serviceIds.forEach(serviceId -> serviceRepo.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại: " + serviceId)));
    }

    @Transactional(readOnly = true)
    public List<org.example.doansummer2026.dto.customervisit.SameDayExaminationServiceResponse>
    getSameDayExaminationServices(UUID customerId) {
        if (!profileRepo.existsById(customerId)) {
            throw new ResourceNotFoundException("Khách hàng không tồn tại: " + customerId);
        }
        Set<UUID> includedServiceIds = new java.util.HashSet<>();
        List<org.example.doansummer2026.dto.customervisit.SameDayExaminationServiceResponse> result =
                new ArrayList<>(sameDayExaminationItems(customerId).stream()
                .filter(item -> item.getService() != null
                        && includedServiceIds.add(item.getService().getServiceId()))
                .map(item -> {
                    var invoice = item.getInvoice();
                    var visit = invoice.getVisit();
                    var service = item.getService();
                    var queue = queueTicketRepo
                            .findTopByVisit_VisitIdAndService_ServiceIdOrderByCreatedAtDesc(
                                    visit.getVisitId(), service.getServiceId())
                            .orElse(null);
                    String visitCode = "VIS-" + visit.getVisitId().toString()
                            .replace("-", "").substring(0, 8).toUpperCase();
                    String state = queue != null ? queueStatusLabel(queue.getStatus())
                            : (invoice.getStatus() == org.example.doansummer2026.enums.InvoiceStatus.PENDING
                            ? "Chờ thanh toán" : "Đã thanh toán");
                    return new org.example.doansummer2026.dto.customervisit.SameDayExaminationServiceResponse(
                            service.getServiceId(), service.getServiceCode(), service.getName(),
                            visit.getVisitId(), visitCode, visit.getStatus(),
                            queue != null ? queue.getStatus() : null,
                            visit.getCheckInTime(), true,
                            "Đã đăng ký hôm nay · " + visitCode + " · " + state);
                })
                .toList());
        queueTicketRepo.findSameDayPatientExaminationTickets(customerId, clinicToday()).stream()
                .filter(ticket -> ticket.getService() != null
                        && hasActiveExaminationInvoice(ticket)
                        && includedServiceIds.add(ticket.getService().getServiceId()))
                .map(ticket -> {
                    CustomerVisit visit = ticket.getVisit();
                    MedicalService service = ticket.getService();
                    String visitCode = "VIS-" + visit.getVisitId().toString()
                            .replace("-", "").substring(0, 8).toUpperCase();
                    return new org.example.doansummer2026.dto.customervisit.SameDayExaminationServiceResponse(
                            service.getServiceId(), service.getServiceCode(), service.getName(),
                            visit.getVisitId(), visitCode, visit.getStatus(), ticket.getStatus(),
                            visit.getCheckInTime(), true,
                            "Đã đăng ký hôm nay · " + visitCode + " · " + queueStatusLabel(ticket.getStatus()));
                })
                .forEach(result::add);
        return result;
    }

    private String queueStatusLabel(org.example.doansummer2026.enums.QueueStatus status) {
        if (status == null) return "Đã đăng ký";
        return switch (status) {
            case BLOCKED -> "Chưa đến lượt";
            case WAITING -> "Đang chờ gọi";
            case CALLED -> "Đã gọi";
            case IN_PROGRESS -> "Đang khám";
            case WAITING_FOR_TEST -> "Chờ cận lâm sàng";
            case TEST_DONE -> "Chờ quay lại bác sĩ";
            case DONE -> "Đã hoàn thành";
            case SKIPPED -> "Vắng mặt";
        };
    }

    public void validateNoSameDayExaminationRegistration(UUID customerId, List<UUID> requestedServiceIds) {
        Set<UUID> requestedExaminations = requestedServiceIds.stream()
                .distinct()
                .map(serviceId -> serviceRepo.findById(serviceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Dịch vụ không tồn tại: " + serviceId)))
                .filter(service -> service.getDepartmentType() != null
                        && service.getDepartmentType().normalized()
                        == org.example.doansummer2026.enums.DepartmentType.EXAMINATION)
                .map(MedicalService::getServiceId)
                .collect(java.util.stream.Collectors.toSet());
        if (requestedExaminations.isEmpty()) return;

        sameDayExaminationItems(customerId).stream()
                .filter(item -> item.getService() != null
                        && requestedExaminations.contains(item.getService().getServiceId()))
                .findFirst()
                .ifPresent(item -> {
                    CustomerVisit existingVisit = item.getInvoice().getVisit();
                    String visitCode = "VIS-" + existingVisit.getVisitId().toString()
                            .replace("-", "").substring(0, 8).toUpperCase();
                    throw new ConflictException("Dịch vụ " + item.getService().getName()
                            + " đã được đăng ký hôm nay trong lượt " + visitCode
                            + ". Vui lòng chọn dịch vụ khám khác");
                });
        queueTicketRepo.findSameDayPatientExaminationTickets(customerId, clinicToday()).stream()
                .filter(ticket -> ticket.getService() != null
                        && hasActiveExaminationInvoice(ticket)
                        && requestedExaminations.contains(ticket.getService().getServiceId()))
                .findFirst()
                .ifPresent(ticket -> {
                    String visitCode = "VIS-" + ticket.getVisit().getVisitId().toString()
                            .replace("-", "").substring(0, 8).toUpperCase();
                    throw new ConflictException("Dịch vụ " + ticket.getService().getName()
                            + " đã được đăng ký hôm nay trong lượt " + visitCode
                            + ". Vui lòng chọn dịch vụ khám khác");
                });
    }

    private List<org.example.doansummer2026.model.InvoiceItem> sameDayExaminationItems(UUID customerId) {
        LocalDate today = clinicToday();
        return invoiceItemRepo.findSameDayExaminationRegistrations(
                customerId, today.atStartOfDay(), today.plusDays(1).atStartOfDay(), null);
    }

    private boolean hasActiveExaminationInvoice(org.example.doansummer2026.model.QueueTicket ticket) {
        return ticket.getVisit() != null && ticket.getService() != null
                && invoiceItemRepo.findDistinctExaminationServiceIdsByVisit(
                        ticket.getVisit().getVisitId(), null)
                .contains(ticket.getService().getServiceId());
    }

    private void validateGuestInformation(CustomerVisitCreateRequest req, String phone) {
        String fullName = req.guestFullName() == null ? "" : req.guestFullName().trim();
        if (fullName.length() < 2) {
            throw new BadRequestException("Vui lòng nhập họ tên bệnh nhân");
        }
        if (fullName.codePoints().anyMatch(Character::isDigit)) {
            throw new BadRequestException("Họ tên không được chứa chữ số");
        }
        if (phone != null && !phone.isBlank() && !phone.matches("^0\\d{9,10}$")) {
            throw new BadRequestException("Số điện thoại Việt Nam không hợp lệ");
        }
        String email = normalizeEmail(req.guestEmail());
        if ((phone == null || phone.isBlank()) && email == null && req.guestDateOfBirth() == null) {
            throw new BadRequestException("Ngày sinh là bắt buộc khi bệnh nhân chưa có số điện thoại và email");
        }
        if (req.guestGender() == null || req.guestGender() == Gender.OTHER) {
            throw new BadRequestException("Hệ thống chỉ hỗ trợ giới tính Nam hoặc Nữ");
        }
    }

    private void applyPatientIntake(Profile customer, CustomerVisitCreateRequest req) {
        String fullName = req.guestFullName() == null ? "" : req.guestFullName().trim().replaceAll("\\s+", " ");
        if (fullName.length() < 2 || fullName.length() > 100) {
            throw new BadRequestException("Họ tên phải có từ 2 đến 100 ký tự");
        }
        if (fullName.codePoints().anyMatch(Character::isDigit)) {
            throw new BadRequestException("Họ tên không được chứa chữ số");
        }
        String phone = normalizeVietnamesePhone(req.guestPhone());
        if (phone != null && !phone.matches("^0\\d{9,10}$")) {
            throw new BadRequestException("Số điện thoại Việt Nam không hợp lệ");
        }
        if (phone != null) {
            profileRepo.findFirstByPhoneIn(phoneVariants(phone)).ifPresent(other -> {
                if (!other.getProfileId().equals(customer.getProfileId())) {
                    throw new ConflictException("Số điện thoại đã được sử dụng bởi hồ sơ bệnh nhân khác");
                }
            });
        }
        String email = normalizeEmail(req.guestEmail());
        if (customer.getAccount() != null && phone == null && email == null) {
            throw new BadRequestException("Hồ sơ đã có tài khoản phải giữ ít nhất một số điện thoại hoặc email đăng nhập");
        }
        if (email != null) {
            profileRepo.findFirstByEmailIgnoreCase(email).ifPresent(other -> {
                if (!other.getProfileId().equals(customer.getProfileId())) {
                    throw new ConflictException("Email đã được sử dụng bởi hồ sơ bệnh nhân khác");
                }
            });
        }
        if (req.guestDateOfBirth() != null && !req.guestDateOfBirth().isBefore(clinicToday())) {
            throw new BadRequestException("Ngày sinh phải là ngày trong quá khứ");
        }
        if (phone == null && email == null && req.guestDateOfBirth() == null) {
            throw new BadRequestException("Ngày sinh là bắt buộc khi bệnh nhân chưa có số điện thoại và email");
        }
        if (req.guestGender() == null || req.guestGender() == Gender.OTHER) {
            throw new BadRequestException("Giới tính chỉ được chọn Nam hoặc Nữ");
        }
        List<String> allergies = org.example.doansummer2026.dto.medicalrecord.PatientAllergyResponse
                .normalize(req.guestAllergies());
        if (allergies.size() > 20 || allergies.stream().anyMatch(item -> item.length() > 100)) {
            throw new BadRequestException("Danh sách dị ứng không hợp lệ");
        }
        if (req.allergyStatus() == AllergyStatus.REPORTED && allergies.isEmpty()) {
            throw new BadRequestException("Vui lòng nhập ít nhất một dị ứng đã ghi nhận");
        }

        customer.setFullName(fullName);
        customer.setPhone(phone);
        customer.setEmail(email);
        customer.setDateOfBirth(req.guestDateOfBirth());
        customer.setGender(req.guestGender());
        customer.setAddress(req.guestAddress() == null || req.guestAddress().isBlank()
                ? null : req.guestAddress().trim());
        customer.setBloodType(req.guestBloodType());
        if (req.allergyStatus() != null) {
            customer.setAllergies(switch (req.allergyStatus()) {
                case UNVERIFIED -> null;
                case NONE_REPORTED -> "";
                case REPORTED -> String.join("\n", allergies);
            });
        }
        profileRepo.save(customer);
    }

    private String normalizeVietnamesePhone(String value) {
        if (value == null) return null;
        String phone = value.trim().replaceAll("[\\s.-]", "");
        if (phone.isBlank()) return null;
        return phone.startsWith("+84") ? "0" + phone.substring(3) : phone;
    }

    private String normalizeEmail(String value) {
        return value == null || value.isBlank()
                ? null : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private Profile findExistingGuestProfile(String phone, String email) {
        Profile byPhone = phone == null ? null
                : profileRepo.findFirstByPhoneIn(phoneVariants(phone)).orElse(null);
        Profile byEmail = email == null ? null
                : profileRepo.findFirstByEmailIgnoreCase(email).orElse(null);
        if (byPhone != null && byEmail != null
                && !byPhone.getProfileId().equals(byEmail.getProfileId())) {
            throw new ConflictException("Số điện thoại và email đang thuộc hai hồ sơ bệnh nhân khác nhau");
        }
        return byPhone != null ? byPhone : byEmail;
    }

    private List<String> phoneVariants(String normalizedPhone) {
        if (normalizedPhone == null || normalizedPhone.isBlank()) return List.of();
        if (normalizedPhone.startsWith("0")) {
            return List.of(normalizedPhone, "+84" + normalizedPhone.substring(1));
        }
        return List.of(normalizedPhone);
    }

    private String profileSnapshot(Profile profile) {
        try {
            return objectMapper.writeValueAsString(java.util.Map.ofEntries(
                    java.util.Map.entry("profileId", profile.getProfileId().toString()),
                    java.util.Map.entry("fullName", java.util.Objects.toString(profile.getFullName(), "")),
                    java.util.Map.entry("phone", java.util.Objects.toString(profile.getPhone(), "")),
                    java.util.Map.entry("email", java.util.Objects.toString(profile.getEmail(), "")),
                    java.util.Map.entry("dateOfBirth", java.util.Objects.toString(profile.getDateOfBirth(), "")),
                    java.util.Map.entry("gender", java.util.Objects.toString(profile.getGender(), "")),
                    java.util.Map.entry("address", java.util.Objects.toString(profile.getAddress(), "")),
                    java.util.Map.entry("bloodType", java.util.Objects.toString(profile.getBloodType(), "")),
                    java.util.Map.entry("allergies", java.util.Objects.toString(profile.getAllergies(), ""))));
        } catch (Exception ignored) {
            return null;
        }
    }

    private void scheduleProfileAudit(UUID profileId, org.example.doansummer2026.model.StaffInfo actor,
                                      String oldValueJson, String newValueJson) {
        UUID actorAccountId = actor != null && actor.getProfile() != null && actor.getProfile().getAccount() != null
                ? actor.getProfile().getAccount().getAccountId() : null;
        Runnable writeAudit = () -> auditLogService.create(
                new org.example.doansummer2026.dto.auditlog.AuditLogCreateRequest(
                        org.example.doansummer2026.enums.AuditAction.UPDATE,
                        "Profile", profileId.toString(), actorAccountId,
                        null, null, oldValueJson, newValueJson,
                        "Lễ tân cập nhật hồ sơ bệnh nhân khi tạo phiếu khám"));
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override public void afterCommit() { writeAudit.run(); }
                    });
        } else {
            writeAudit.run();
        }
    }

    private void validateAppointmentForCheckIn(Appointment appointment, Profile customer) {
        if (appointment.getStatus() != AppointmentStatus.PENDING
                && appointment.getStatus() != AppointmentStatus.RESCHEDULED) {
            throw new ConflictException("Lịch hẹn không còn ở trạng thái chờ tiếp nhận");
        }
        if (repo.findByAppointment_AppointmentId(appointment.getAppointmentId()).isPresent()) {
            throw new ConflictException("Lịch hẹn đã được tiếp nhận trước đó");
        }
        if (appointment.getScheduledAt() == null) {
            throw new BadRequestException("Lịch hẹn chưa có ngày khám hợp lệ");
        }
        if (appointment.getScheduledAt().toLocalDate().isBefore(clinicToday())) {
            throw new BadRequestException("Không thể tiếp nhận lịch hẹn đã quá ngày. Lịch hẹn này vào ngày "
                    + appointment.getScheduledAt().toLocalDate());
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

    private void validateCurrentExaminationAvailability(MedicalService service) {
        if (service.getDepartmentType() == null
                || service.getDepartmentType().normalized()
                != org.example.doansummer2026.enums.DepartmentType.EXAMINATION) return;

        LocalDate today = clinicToday();
        java.time.LocalTime now = java.time.LocalTime.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        var activeShift = shiftConfigRepository.findAllByIsActiveTrueOrderByStartTimeAsc().stream()
                .filter(shift -> {
                    var resolved = shiftScheduleResolver.resolve(shift, today);
                    return resolved.available() && resolved.startTime() != null && resolved.endTime() != null
                            && !now.isBefore(resolved.startTime()) && now.isBefore(resolved.endTime());
                })
                .findFirst()
                .orElseThrow(() -> new ConflictException(
                        "Hiện không nằm trong ca làm việc đang hoạt động của phòng khám"));
        var evaluation = serviceAvailabilityService.evaluate(service, today, activeShift, false);
        if (!evaluation.available()) {
            throw new ConflictException("Dịch vụ " + service.getName()
                    + " chưa có bác sĩ đủ điều kiện trong ca hiện tại"
                    + (evaluation.reason() == null ? "" : " (" + evaluation.reason().name() + ")"));
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



