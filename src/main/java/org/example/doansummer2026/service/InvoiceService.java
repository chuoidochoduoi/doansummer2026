package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.invoice.InvoiceCreateRequest;
import org.example.doansummer2026.dto.invoice.InvoiceItemCreateRequest;
import org.example.doansummer2026.dto.invoice.InvoiceResponse;
import org.example.doansummer2026.dto.invoice.InvoiceUpdateRequest;
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
import org.example.doansummer2026.enums.TransactionStatus;
import org.example.doansummer2026.repository.InvoiceItemRepository;
import org.example.doansummer2026.repository.InvoiceRepository;
import org.example.doansummer2026.repository.MedicalRecordRepository;
import org.example.doansummer2026.repository.MedicalServiceRepository;
import org.example.doansummer2026.repository.CustomerVisitRepository;
import org.example.doansummer2026.repository.AccountRepository;
import org.example.doansummer2026.repository.ProfileRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.example.doansummer2026.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.InvoiceServiceInterface;
import org.example.doansummer2026.service.interfaces.QueueTicketServiceInterface;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> search(UUID customerId, InvoiceStatus status,
                                                 LocalDate from, LocalDate to, Pageable pageable) {
        Page<Invoice> page = repo.search(customerId, status, from, to, pageable);
        return PageResponse.from(page, i -> InvoiceResponse.from(i, true));
    }

    @Transactional(readOnly = true)
    public InvoiceResponse get(UUID id) {
        return InvoiceResponse.from(findById(id), true);
    }

    public InvoiceResponse create(InvoiceCreateRequest req) {
        Profile customer = profileRepo.findById(req.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Khach hang khong ton tai: " + req.customerId()));
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
                .customer(customer)
                .visit(visit)
                .medicalRecord(record)
                .issueDate(LocalDate.now())
                .dueDate(req.dueDate())
                .subtotal(BigDecimal.ZERO)
                .discount(req.discount() != null ? req.discount() : BigDecimal.ZERO)
                .tax(req.tax() != null ? req.tax() : BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .paidAmount(BigDecimal.ZERO)
                .status(InvoiceStatus.DRAFT)
                .note(req.note())
                .issuedBy(issuedBy)
                .items(new ArrayList<>())
                .build();
        Invoice saved = repo.save(invoice);
        if (req.items() != null) {
            for (InvoiceItemCreateRequest itemReq : req.items()) {
                saved.getItems().add(buildItem(saved, itemReq));
            }
        }
        recalculateTotals(saved);
        return InvoiceResponse.from(repo.save(saved), true);
    }

    public InvoiceResponse update(UUID id, InvoiceUpdateRequest req) {
        Invoice i = findById(id);
        if (i.getStatus() != InvoiceStatus.DRAFT) {
            throw new ConflictException("Chi sua duoc hoa don o trang thai DRAFT; hien tai: " + i.getStatus());
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
        return InvoiceResponse.from(repo.save(i), true);
    }

    public InvoiceResponse issue(UUID id) {
        Invoice i = findById(id);
        if (i.getStatus() != InvoiceStatus.DRAFT) {
            throw new ConflictException("Chi xuat hoa don o trang thai DRAFT; hien tai: " + i.getStatus());
        }
        if (i.getItems().isEmpty()) {
            throw new BadRequestException("Khong the xuat hoa don khong co dong nao");
        }
        i.setStatus(InvoiceStatus.ISSUED);
        return InvoiceResponse.from(repo.save(i), true);
    }

    public InvoiceResponse cancel(UUID id) {
        Invoice i = findById(id);
        if (i.getStatus() == InvoiceStatus.PAID || i.getStatus() == InvoiceStatus.REFUNDED) {
            throw new ConflictException("Khong the huy hoa don da thanh toan: " + i.getStatus());
        }
        boolean hasSuccess = transactionRepo.findByInvoice_InvoiceId(id).stream()
                .anyMatch(t -> t.getStatus() == TransactionStatus.SUCCESS);
        if (hasSuccess) {
            throw new ConflictException("Khong the huy - da co giao dich thanh cong");
        }
        i.setStatus(InvoiceStatus.CANCELLED);
        return InvoiceResponse.from(repo.save(i), true);
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

    /** Recalculate paidAmount + status tuyen tu cac transaction SUCCESS. */
    public void recalculatePaidAmount(UUID invoiceId) {
        Invoice i = findById(invoiceId);
        BigDecimal paid = transactionRepo.findByInvoice_InvoiceId(invoiceId).stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .map(t -> t.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        i.setPaidAmount(paid);
        if (i.getStatus() == InvoiceStatus.CANCELLED || i.getStatus() == InvoiceStatus.REFUNDED) {
            return;
        }
        int cmp = paid.compareTo(i.getTotalAmount());
        if (cmp >= 0) {
            i.setStatus(InvoiceStatus.PAID);
        } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
            i.setStatus(InvoiceStatus.PARTIALLY_PAID);
        } else if (i.getStatus() == InvoiceStatus.PAID || i.getStatus() == InvoiceStatus.PARTIALLY_PAID) {
            i.setStatus(InvoiceStatus.ISSUED);
        }
        Invoice saved = repo.save(i);
        // Tu dong tao QueueTicket cho moi InvoiceItem khi Invoice duoc thanh toan
        if (saved.getStatus() == InvoiceStatus.PAID && saved.getVisit() != null) {
            createQueueTicketsFromInvoiceItems(saved);
        }
    }

    /** Tao QueueTicket cho moi InvoiceItem (phai co service va service.department). */
    private void createQueueTicketsFromInvoiceItems(Invoice invoice) {
        for (InvoiceItem item : invoice.getItems()) {
            if (item.getService() == null) continue;
            MedicalService service = item.getService();
            if (service.getDepartment() == null) continue;
            queueTicketService.create(new org.example.doansummer2026.dto.queueTicket.QueueTicketCreateRequest(
                    invoice.getVisit().getVisitId(),
                    service.getDepartment().getDepartmentId(),
                    service.getServiceId(),
                    null
            ));
        }
    }

    // --- helpers ---

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
}
