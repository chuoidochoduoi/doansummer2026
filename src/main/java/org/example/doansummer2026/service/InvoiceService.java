package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.invoice.InvoiceCreateRequest;
import org.example.doansummer2026.dto.invoice.InvoiceItemCreateRequest;
import org.example.doansummer2026.dto.invoice.InvoiceResponse;
import org.example.doansummer2026.dto.invoice.InvoiceUpdateRequest;
import org.example.doansummer2026.dto.invoice.InvoiceInsuranceRequest;
import org.example.doansummer2026.dto.invoice.PaymentHistoryResponse;
import org.example.doansummer2026.dto.invoice.ReceiptDetailResponse;
import org.example.doansummer2026.dto.invoice.ReceiptPrintResponse;
import org.example.doansummer2026.enums.PaymentMethod;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Invoice;
import org.example.doansummer2026.model.InvoiceItem;
import org.example.doansummer2026.enums.InvoiceStatus;
import org.example.doansummer2026.model.MedicalRecord;
import org.example.doansummer2026.model.MedicalService;
import org.example.doansummer2026.model.CustomerVisit;
import org.example.doansummer2026.model.Profile;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.model.Department;
import org.example.doansummer2026.enums.DepartmentType;
import org.example.doansummer2026.enums.DepartmentStatus;
import org.example.doansummer2026.enums.TransactionStatus;
import org.example.doansummer2026.repository.InvoiceItemRepository;
import org.example.doansummer2026.repository.InsuranceRepository;
import org.example.doansummer2026.repository.InsuranceRuleRepository;
import org.example.doansummer2026.repository.InvoiceRepository;
import org.example.doansummer2026.repository.MedicalRecordRepository;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.AccountRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.example.doansummer2026.repository.TransactionRepository;
import org.example.doansummer2026.repository.QueueTicketRepository;
import org.example.doansummer2026.repository.TestRequestRepository;
import org.example.doansummer2026.repository.DepartmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.InvoiceServiceInterface;
import org.example.doansummer2026.service.interfaces.QueueTicketServiceInterface;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
@RequiredArgsConstructor
public class InvoiceService implements InvoiceServiceInterface {

    private final InvoiceRepository repo;
    private final InvoiceItemRepository itemRepo;
    private final TransactionRepository transactionRepo;
    private final ProfileRepository profileRepo;
    private final CustomerVisitRepository visitRepo;
    private final MedicalRecordRepository recordRepo;
    private final StaffInfoRepository staffRepo;
    private final MedicalServiceRepository serviceRepo;
    private final AccountRepository accountRepo;
    private final QueueTicketService queueTicketService;
    private final TestRequestService testRequestService;
    private final QueueTicketRepository queueTicketRepo;
    private final TestRequestRepository testRequestRepo;
    private final DepartmentRepository departmentRepo;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final InsuranceRepository insuranceRepository;
    private final InsuranceRuleRepository insuranceRuleRepository;
    private final BhxhIntegrationService bhxhIntegrationService;

    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> search(UUID customerId, InvoiceStatus status,
                                                 String search, String category,
                                                 LocalDate from, LocalDate to, Pageable pageable) {
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim().toLowerCase();
        String normalizedCategory = category == null || category.isBlank() ? null : category.trim().toLowerCase();
        Specification<Invoice> spec = (root, query, cb) -> cb.conjunction();

        if (customerId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("customer").get("profileId"), customerId));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (normalizedSearch != null) {
            String pattern = "%" + normalizedSearch + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("invoiceCode")), pattern),
                    cb.like(cb.lower(root.get("customer").get("fullName")), pattern),
                    cb.like(cb.lower(root.get("customer").get("phone")), pattern)
            ));
        }
        if (normalizedCategory != null) {
            String pattern = "%" + normalizedCategory + "%";
            spec = spec.and((root, query, cb) -> {
                var item = root.join("items", jakarta.persistence.criteria.JoinType.LEFT);
                var medicalService = item.join("service", jakarta.persistence.criteria.JoinType.LEFT);
                query.distinct(true);
                return cb.or(
                        cb.like(cb.lower(item.get("serviceSnapshot")), pattern),
                        cb.like(cb.lower(medicalService.get("name")), pattern)
                );
            });
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("issueDate"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("issueDate"), to));
        }

        Pageable sortedPageable = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Invoice> page = repo.findAll(spec, sortedPageable);
        return PageResponse.from(page, i -> {
            List<UUID> txIds = transactionRepo.findByInvoice_InvoiceId(i.getInvoiceId()).stream()
                    .map(t -> t.getTransactionId())
                    .toList();
            return InvoiceResponse.from(i, txIds);
        });
    }

    @Transactional(readOnly = true)
    public InvoiceResponse get(UUID id) {
        Invoice i = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hoa don khong ton tai: " + id));
        List<UUID> txIds = transactionRepo.findByInvoice_InvoiceId(id).stream()
                .map(t -> t.getTransactionId())
                .toList();
        return InvoiceResponse.from(i, txIds);
    }

    public InvoiceResponse create(InvoiceCreateRequest req) {
        // Customer co the null cho guest vang lai check-in
        Profile customer = null;
        if (req.customerId() != null) {
            customer = profileRepo.findById(req.customerId()).orElse(null);
        }
        CustomerVisit visit = null;
        if (req.visitId() != null) {
            visit = visitRepo.findById(req.visitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Luot kham khong ton tai: " + req.visitId()));
        }
        MedicalRecord record = null;
        if (req.medicalRecordId() != null) {
            record = recordRepo.findById(req.medicalRecordId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Ho so benh an khong ton tai: " + req.medicalRecordId()));
        }
        StaffInfo issuedBy = null;
        if (req.issuedById() != null) {
            issuedBy = staffRepo.findById(req.issuedById()).orElse(null);
        }
        Invoice invoice = Invoice.builder()
                .invoiceCode(generateInvoiceCode())
                .customer(customer) // Co the null cho guest
                .visit(visit)
                .medicalRecord(record)
                .issueDate(LocalDate.now())
                .dueDate(req.dueDate())
                .subtotal(BigDecimal.ZERO)
                .discount(req.discount() != null ? req.discount() : BigDecimal.ZERO)
                .tax(req.tax() != null ? req.tax() : BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .status(InvoiceStatus.PENDING)
                .note(req.note())
                .issuedBy(issuedBy)
                .items(new ArrayList<>())
                .build();
        Invoice saved = repo.save(invoice);
        if (req.items() != null) {
            List<InvoiceItem> persistedItems = new ArrayList<>();
            for (InvoiceItemCreateRequest itemReq : req.items()) {
                /*
                 * Invoice.items la phia mappedBy. Khong chi dua vao cascade cua
                 * collection nay: o mot so luong tao luot kham (dac biet guest),
                 * Invoice da duoc persist truoc khi item duoc them vao collection.
                 * Luu owner InvoiceItem mot cach tuong minh de dam bao dong dich vu
                 * ton tai truoc khi thu ngan thanh toan va dieu phoi hang cho.
                 */
                InvoiceItem item = buildItem(saved, itemReq);
                persistedItems.add(itemRepo.save(item));
            }
            saved.getItems().addAll(persistedItems);
        }
        recalculateTotals(saved);
        Invoice finalSaved = repo.save(saved);
        notifyCashiers(finalSaved);
        return InvoiceResponse.from(finalSaved);
    }
    
    private void notifyCashiers(Invoice invoice) {
        String patientName = invoice.getCustomer() != null ? invoice.getCustomer().getFullName() : (invoice.getVisit() != null && invoice.getVisit().getAppointment() != null ? invoice.getVisit().getAppointment().getGuestFullName() : "Khach");
        if (patientName == null) patientName = "Khach";
        String content = String.format("Co hoa don moi (Ma: %s) can thanh toan tu benh nhan %s", invoice.getInvoiceCode(), patientName);
        
        List<StaffInfo> cashiers = staffRepo.findAllBySystemRoleIn(List.of(org.example.doansummer2026.enums.SystemRole.CASHIER));
        for (StaffInfo staff : cashiers) {
            if (staff.getProfile() != null) {
                try {
                    notificationService.create(new org.example.doansummer2026.dto.notification.NotificationCreateRequest(
                            staff.getProfile().getProfileId(),
                            org.example.doansummer2026.enums.NotificationType.GENERAL,
                            org.example.doansummer2026.enums.NotificationChannel.IN_APP,
                            "Hoa don moi",
                            content,
                            "Invoice",
                            invoice.getInvoiceId()
                    ));
                } catch (Exception e) {
                    // Ignore
                }
            }
        }
    }

    public InvoiceResponse update(UUID id, InvoiceUpdateRequest req) {
        Invoice i = findById(id);
        if (i.getStatus() != InvoiceStatus.PENDING) {
            throw new ConflictException("Chi sua duoc hoa don o trang thai PENDING; hien tai: " + i.getStatus());
        }
        if (req.dueDate() != null) i.setDueDate(req.dueDate());
        if (req.discount() != null) i.setDiscount(req.discount());
        if (req.tax() != null) i.setTax(req.tax());
        if (req.note() != null) i.setNote(req.note());
        if (req.items() != null && !req.items().isEmpty()) {
            itemRepo.deleteAll(i.getItems());
            i.getItems().clear();
            for (InvoiceItemCreateRequest itemReq : req.items()) {
                i.getItems().add(buildItem(i, itemReq));
            }
        }
        recalculateTotals(i);
        return InvoiceResponse.from(repo.save(i));
    }

    public InvoiceResponse applyInsurance(UUID id, InvoiceInsuranceRequest req) {
        Invoice invoice = repo.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hoa don khong ton tai: " + id));
        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new ConflictException("Chi co the ap dung BHYT khi hoa don dang cho thanh toan");
        }

        var insurance = insuranceRepository.findById(req.insuranceId())
                .orElseThrow(() -> new ResourceNotFoundException("Bao hiem khong ton tai: " + req.insuranceId()));
        var verification = bhxhIntegrationService.checkBhytCard(req.bhytCode().trim());
        if (!verification.isValid()) {
            throw new BadRequestException(verification.message());
        }
        if (verification.insuranceId() != null
                && !verification.insuranceId().equals(insurance.getInsuranceId())) {
            throw new BadRequestException("Ma the khong thuoc loai bao hiem da chon");
        }

        var rules = insuranceRuleRepository.findByInsurance_InsuranceId(insurance.getInsuranceId());
        BigDecimal totalBhyt = BigDecimal.ZERO;
        for (InvoiceItem item : invoice.getItems()) {
            DepartmentType type = item.getService() != null ? item.getService().getDepartmentType() : null;
            BigDecimal rate = rules.stream()
                    .filter(rule -> type != null && rule.getDepartmentType() == type)
                    .map(rule -> rule.getDiscountPercent() != null ? rule.getDiscountPercent() : BigDecimal.ZERO)
                    .findFirst()
                    .orElse(BigDecimal.ZERO);
            BigDecimal lineTotal = item.getLineTotal() != null ? item.getLineTotal() : BigDecimal.ZERO;
            BigDecimal bhytAmount = lineTotal.multiply(rate)
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            item.setDiscountPercent(rate);
            item.setDiscountAmount(bhytAmount);
            item.setBhytFund(bhytAmount);
            item.setFinalPrice(lineTotal.subtract(bhytAmount));
            totalBhyt = totalBhyt.add(bhytAmount);
        }

        invoice.getCustomer().setInsuranceId(req.bhytCode().trim());
        profileRepo.save(invoice.getCustomer());
        invoice.setDiscount(totalBhyt);
        recalculateTotals(invoice);
        return InvoiceResponse.from(repo.save(invoice));
    }

    public InvoiceResponse issue(UUID id) {
        Invoice i = findById(id);
        if (i.getStatus() != InvoiceStatus.PENDING) {
            throw new ConflictException("Chi xuat hoa don o trang thai PENDING; hien tai: " + i.getStatus());
        }
        if (i.getItems().isEmpty()) {
            throw new BadRequestException("Khong the xuat hoa don khong co dong nao");
        }
        i.setStatus(InvoiceStatus.PENDING);
        return InvoiceResponse.from(repo.save(i));
    }

    public InvoiceResponse cancel(UUID id) {
        Invoice i = repo.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hoa don khong ton tai: " + id));
        if (i.getStatus() == InvoiceStatus.PAID) {
            throw new ConflictException("Khong the huy hoa don da thanh toan: " + i.getStatus());
        }
        boolean hasSuccess = transactionRepo.findByInvoice_InvoiceId(id).stream()
                .anyMatch(t -> t.getStatus() == TransactionStatus.SUCCESS);
        if (hasSuccess) {
            throw new ConflictException("Khong the huy - da co giao dich thanh cong");
        }
        i.setStatus(InvoiceStatus.CANCELLED);
        return InvoiceResponse.from(repo.save(i));
    }

    public InvoiceResponse pay(UUID id, UUID receivedById) {
        Invoice i = repo.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hoa don khong ton tai: " + id));
        if (i.getStatus() == InvoiceStatus.PAID) {
            /*
             * Thanh toan la thao tac idempotent. Cac hoa don da PAID tu phien
             * ban cu co the chua sinh TestRequest/QueueTicket; cho phep goi lai
             * de tu phuc hoi workflow, tuyet doi khong tao giao dich thu tien moi.
             */
            createQueueTicketsFromInvoiceItems(i);
            return InvoiceResponse.from(i);
        }
        if (i.getStatus() == InvoiceStatus.CANCELLED) {
            throw new ConflictException("Khong the thanh toan hoa don da huy: " + i.getStatus());
        }
        i.setPaidAmount(i.getTotalAmount());
        i.setStatus(InvoiceStatus.PAID);
        Invoice saved = repo.save(i);
        StaffInfo cashier = receivedById != null ? staffRepo.findById(receivedById).orElse(null) : null;
        if (transactionRepo.findTopByInvoice_InvoiceIdAndStatusOrderByPaidAtDesc(id, TransactionStatus.SUCCESS).isEmpty()) {
            transactionRepo.save(org.example.doansummer2026.model.Transaction.builder()
                    .invoice(saved).transactionCode("PAY-" + saved.getInvoiceCode())
                    .amount(saved.getTotalAmount()).paymentMethod(PaymentMethod.CASH)
                    .status(TransactionStatus.SUCCESS).paidAt(java.time.LocalDateTime.now())
                    .receivedBy(cashier).note("Thanh toan tai quay").build());
        }
        // Luon nap lai hoa don trong ham dieu phoi. Entity vua save co the chua
        // mang quan he visit do LAZY loading, dan den hoa don CLS da PAID nhung
        // bi bo qua viec tao hang cho.
        createQueueTicketsFromInvoiceItems(saved);
        return InvoiceResponse.from(saved);
    }

    public void delete(UUID id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Hoa don khong ton tai: " + id);
        }
        repo.deleteById(id);
    }

    public Invoice findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hoa don khong ton tai: " + id));
    }

    @Transactional(readOnly = true)
    public ReceiptPrintResponse getReceiptPrintData(UUID id) {
        Invoice invoice = repo.getWithDetailsByInvoiceId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hoa don khong ton tai: " + id));
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            throw new ConflictException("Chi in phieu thu sau khi hoa don da thanh toan");
        }
        var payment = transactionRepo.findTopByInvoice_InvoiceIdAndStatusOrderByPaidAtDesc(
                id, TransactionStatus.SUCCESS).orElse(null);
        return ReceiptPrintResponse.from(invoice, payment);
    }

    /** Recalculate paidAmount + status tuyen tu cac transaction SUCCESS. */
    public void recalculatePaidAmount(UUID invoiceId) {
        Invoice i = repo.findByIdForUpdate(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Hoa don khong ton tai: " + invoiceId));
        BigDecimal paid = transactionRepo.findByInvoice_InvoiceId(invoiceId).stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        i.setPaidAmount(paid);
        if (i.getStatus() == InvoiceStatus.CANCELLED) {
            return;
        }
        int cmp = paid.compareTo(i.getTotalAmount());
        if (cmp >= 0) {
            i.setStatus(InvoiceStatus.PAID);
        }  else if (i.getStatus() == InvoiceStatus.PAID ) {
            i.setStatus(InvoiceStatus.PENDING);
        }
        Invoice saved = repo.save(i);
        // Dung cung mot duong dieu phoi voi thanh toan tai quay; ham ben duoi tu
        // nap quan he visit thay vi phu thuoc vao entity LAZY vua save.
        if (saved.getStatus() == InvoiceStatus.PAID) {
            createQueueTicketsFromInvoiceItems(saved);
        }
    }

    /**
     * Tao QueueTicket hoac TestRequest tu cac InvoiceItem sau khi thanh toan.
     * - CLINICAL_EXAM: tao QueueTicket (xep hang cho bac si kham).
     * - LAB_TEST, IMAGING, PROCEDURE: tao TestRequest (gui vao phong xet nghiem/CDHA/thu thuat tuong ung).
     *
     * Luong: Invoice(paid) -> TestRequest(PENDING = hang cho) -> TestResult -> TestRequest(COMPLETED).
     * Moi TestRequest duoc lien ket voi InvoiceItem tuong ung de trace.
     */
    private void createQueueTicketsFromInvoiceItems(Invoice invoice) {
        // Load invoice voi items, service, visit, medicalRecord, issuedBy
        Invoice loaded = repo.getWithDetailsByInvoiceId(invoice.getInvoiceId())
                .orElse(invoice);

        UUID visitId = loaded.getVisit() != null ? loaded.getVisit().getVisitId() : null;
        UUID medicalRecordId = loaded.getMedicalRecord() != null
                ? loaded.getMedicalRecord().getRecordId()
                : (visitId != null ? recordRepo.findFirstByVisit_VisitIdOrderByCreatedAtDesc(visitId)
                        .map(record -> record.getRecordId()).orElse(null) : null);
        UUID requestedById = loaded.getIssuedBy() != null ? loaded.getIssuedBy().getStaffId()
                : transactionRepo.findTopByInvoice_InvoiceIdAndStatusOrderByPaidAtDesc(
                        loaded.getInvoiceId(), TransactionStatus.SUCCESS)
                        .map(transaction -> transaction.getReceivedBy() != null
                                ? transaction.getReceivedBy().getStaffId() : null)
                        .orElse(null);
        boolean workflowActivated = queueTicketRepo.findAllByVisit_VisitId(visitId).stream()
                .anyMatch(ticket -> ticket.getStatus() != org.example.doansummer2026.enums.QueueStatus.BLOCKED
                        && ticket.getStatus() != org.example.doansummer2026.enums.QueueStatus.DONE
                        && ticket.getStatus() != org.example.doansummer2026.enums.QueueStatus.SKIPPED
                        && ticket.getStatus() != org.example.doansummer2026.enums.QueueStatus.WAITING_FOR_TEST)
                || testRequestRepo.findAllByMedicalRecord_Visit_VisitId(visitId).stream()
                .anyMatch(test -> test.getStatus() == org.example.doansummer2026.enums.TestRequestStatus.PENDING
                        || test.getStatus() == org.example.doansummer2026.enums.TestRequestStatus.IN_PROGRESS);

        // Doc truc tiep tu InvoiceItem. Day la diem quan trong: Invoice.items la
        // mappedBy + LAZY; neu collection cua entity vua luu chua dong bo thi vong
        // lap rong va thanh toan da PAID nhung khong sinh TestRequest nao.
        var workflowItems = new ArrayList<>(itemRepo.findAllWithServiceByInvoiceId(loaded.getInvoiceId()));
        if (workflowItems.isEmpty()) {
            if (visitId != null) {
                throw new BadRequestException("Hoa don cua luot kham chua co dich vu. Vui long them dich vu truoc khi thanh toan");
            }
            return;
        }
        if (visitId == null) {
            throw new BadRequestException("Hoa don co dich vu nhung chua gan voi luot kham; khong the tao hang cho");
        }
        workflowItems.sort(java.util.Comparator
                .comparing((InvoiceItem item) -> item.getService() != null && item.getService().getWorkflowPriority() != null ? item.getService().getWorkflowPriority() : 1, java.util.Comparator.reverseOrder())
                .thenComparing((InvoiceItem item) -> item.getService() != null && Boolean.TRUE.equals(item.getService().getRequiresDoctorOrder()))
                .thenComparing((InvoiceItem item) -> item.getService() != null && item.getService().getResultWaitMinutes() != null ? item.getService().getResultWaitMinutes() : 0, java.util.Comparator.reverseOrder()));
        int dispatchedItemCount = 0;
        for (InvoiceItem item : workflowItems) {
            MedicalService service = item.getService();
            // Hoa don cu co the chi luu snapshot. Van phai dieu phoi sau thanh toan
            // neu ma dich vu con ton tai trong danh muc.
            if (service == null && item.getServiceCodeSnapshot() != null) {
                service = serviceRepo.findByServiceCode(item.getServiceCodeSnapshot()).orElse(null);
            }
            // Dich vu can lam sang duoc xep phong dong theo danh muc ky thuat,
            // nen khong bat buoc gan san department tren dich vu.
            if (service == null) {
                throw new BadRequestException("Khong xac dinh duoc dich vu cua dong hoa don: "
                        + item.getServiceSnapshot());
            }

            DepartmentType departmentType = service.getDepartmentType();
            if (departmentType == DepartmentType.EXAMINATION) {
                Department performingRoom = selectExaminationRoom(service);
                // CLINICAL_EXAM: tao QueueTicket cho bac si kham
                var ticket = queueTicketService.create(new org.example.doansummer2026.dto.queueTicket.QueueTicketCreateRequest(
                        visitId,
                        performingRoom.getDepartmentId(),
                        service.getServiceId(),
                        null
                ));
                if (workflowActivated) {
                    queueTicketRepo.findById(ticket.ticketId()).ifPresent(blocked -> {
                        blocked.setStatus(org.example.doansummer2026.enums.QueueStatus.BLOCKED);
                        queueTicketRepo.save(blocked);
                    });
                } else {
                    workflowActivated = true;
                }
                dispatchedItemCount++;
            } else if (departmentType != null && departmentType.isParaclinical()) {
                // TestRequestService tim queue theo visit + phong truoc khi tao.
                // Nhieu dich vu cung phong se dung chung mot QueueTicket.
                var createdRequest = testRequestService.createFromPaidInvoice(
                        visitId,
                        medicalRecordId,
                        service.getServiceId(),
                        requestedById,
                        item.getNote() != null ? item.getNote() : service.getName(),
                        item.getItemId()
                );
                // Luot chi co can lam sang chua co ho so. Request dau tien se tao
                // ho so toi thieu; cac dong sau cua cung hoa don dung lai dung ho so do.
                if (medicalRecordId == null) {
                    medicalRecordId = createdRequest.medicalRecordId();
                }
                // Khong block sau khi da tao: trang thai duoc quyet dinh ngay
                // luc TestRequestService tim/tao ticket cua phong.
                workflowActivated = true;
                dispatchedItemCount++;
            } else {
                throw new BadRequestException("Dich vu '" + service.getName()
                        + "' chua co nhom dieu phoi hop le");
            }
        }
        if (dispatchedItemCount == 0) {
            throw new BadRequestException("Khong co dich vu nao duoc dieu phoi sau thanh toan");
        }
    }

    // --- helpers ---

    /**
     * Dịch vụ khám được cấu hình theo chuyên khoa; phòng vật lý được chọn lúc
     * thanh toán để có thể có nhiều phòng cùng một chuyên khoa. Giữ fallback
     * cho dữ liệu dịch vụ cũ còn gắn phòng trực tiếp.
     */
    private Department selectExaminationRoom(MedicalService service) {
        if (service.getDepartment() != null
                && service.getDepartment().getDepartmentType() == DepartmentType.EXAMINATION
                && service.getDepartment().getStatus() != DepartmentStatus.MAINTENANCE
                && service.getDepartment().getHeadDoctor() != null) {
            return service.getDepartment();
        }

        if (service.getRequiredSpecialization() == null) {
            throw new BadRequestException("Dich vu kham benh '" + service.getName()
                    + "' chua duoc cau hinh chuyen khoa phuc vu");
        }

        return departmentRepo.findEligibleExaminationRoomsBySpecialization(
                        service.getRequiredSpecialization().getSpecializationId())
                .stream()
                // Ưu tiên phòng đã có bác sĩ phụ trách, nhưng vẫn điều phối được
                // dữ liệu phòng cũ chưa đồng bộ head_doctor_id.
                .min(java.util.Comparator
                        .comparing((Department room) -> room.getHeadDoctor() == null)
                        .thenComparingLong(room -> queueTicketRepo
                                .countActiveTicketsByDepartment(room.getDepartmentId())))
                .orElseThrow(() -> new BadRequestException("Chua co phong kham san sang "
                        + "cho chuyen khoa '" + service.getRequiredSpecialization().getName()
                        + "' cua dich vu '" + service.getName() + "'"));
    }

    private InvoiceItem buildItem(Invoice invoice, InvoiceItemCreateRequest req) {
        MedicalService service = null;
        if (req.serviceId() != null) {
            service = serviceRepo.findById(req.serviceId()).orElse(null);
        }
        BigDecimal lineTotal = req.unitPrice().multiply(BigDecimal.valueOf(req.quantity()));
        return InvoiceItem.builder()
                .invoice(invoice)
                .service(service)
                .serviceSnapshot(req.serviceSnapshot())
                .serviceCodeSnapshot(req.serviceCodeSnapshot())
                .unitPrice(req.unitPrice())
                .quantity(req.quantity())
                .discountPercent(req.discountPercent() != null ? req.discountPercent() : BigDecimal.ZERO)
                .discountAmount(req.discountAmount() != null ? req.discountAmount() : BigDecimal.ZERO)
                .finalPrice(req.finalPrice() != null ? req.finalPrice() : lineTotal)
                .lineTotal(lineTotal)
                .note(req.note())
                .build();
    }

    private void recalculateTotals(Invoice i) {
        BigDecimal subtotal = i.getItems().stream()
                .map(it -> it.getLineTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        i.setSubtotal(subtotal);
        BigDecimal total = subtotal.subtract(i.getDiscount()).add(i.getTax());
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Tong tien am - kiem tra discount/tax");
        }
        i.setTotalAmount(total);
    }

    private String generateInvoiceCode() {
        String prefix = "INV-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        for (int attempt = 0; attempt < 3; attempt++) {
            String suffix = String.format("%06X",
                    ThreadLocalRandom.current().nextInt(0, 0xFFFFFF));
            String code = prefix + suffix;
            if (!repo.existsByInvoiceCode(code)) return code;
        }
        throw new ConflictException("Khong the sinh invoice code sau 3 lan thu");
    }

    /**
     * Lich su thanh toan cho benh nhan.
     */
    @Transactional(readOnly = true)
    public PageResponse<PaymentHistoryResponse> getPaymentHistoryForPatient(UUID customerId,
                                                                        LocalDate from, LocalDate to,
                                                                        PaymentMethod paymentMethod,
                                                                        Pageable pageable) {
        Pageable sortedPageable = pageable.getSort().isSorted() ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        var page = repo.findAll(searchForPatientSpec(customerId, from, to, paymentMethod), sortedPageable);
        // Eager fetch customer, items de tranh LazyInitializationException
        page.getContent().forEach(invoice -> {
            if (invoice.getCustomer() != null) {
                invoice.getCustomer().getFullName();
            }
            invoice.getItems().size();
        });

        return PageResponse.from(page, invoice -> {
            var latestTx = transactionRepo.findTopByInvoice_InvoiceIdAndStatusOrderByPaidAtDesc(
                    invoice.getInvoiceId(), org.example.doansummer2026.enums.TransactionStatus.SUCCESS);
            return PaymentHistoryResponse.from(invoice, latestTx.orElse(null));
        });
    }

    private org.springframework.data.jpa.domain.Specification<Invoice> searchForPatientSpec(
            UUID customerId, LocalDate from, LocalDate to, PaymentMethod paymentMethod) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (customerId != null) {
                predicates.add(cb.equal(root.get("customer").get("profileId"), customerId));
            }
            predicates.add(cb.equal(root.get("status"), InvoiceStatus.PAID));
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("issueDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("issueDate"), to));
            }
            if (paymentMethod != null) {
                var subquery = query.subquery(UUID.class);
                var tx = subquery.from(org.example.doansummer2026.model.Transaction.class);
                subquery.select(tx.get("invoice").get("invoiceId"));
                subquery.where(
                        cb.equal(tx.get("invoice").get("invoiceId"), root.get("invoiceId")),
                        cb.equal(tx.get("paymentMethod"), paymentMethod),
                        cb.equal(tx.get("status"), TransactionStatus.SUCCESS)
                );
                predicates.add(cb.exists(subquery));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    /**
     * Chi tiet phieu thu cho benh nhan.
     */
    @Transactional(readOnly = true)
    public ReceiptDetailResponse getReceiptDetail(UUID invoiceId, UUID customerId) {
        Invoice invoice = repo.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Hoa don khong ton tai: " + invoiceId));
        // Kiem tra quyen: invoice phai thuoc ve khach hang nay
        if (invoice.getCustomer() == null || !invoice.getCustomer().getProfileId().equals(customerId)) {
            throw new ResourceNotFoundException("Khong tim thay hoa don");
        }
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            throw new ConflictException("Chi co the xem phieu thu cua hoa don da thanh toan");
        }
        return ReceiptDetailResponse.from(invoice);
    }
}
