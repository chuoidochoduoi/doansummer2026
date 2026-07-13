package org.example.doansummer2026.service;

import lombok.RequiredArgsConstructor;
import org.example.doansummer2026.common.PageResponse;
import org.example.doansummer2026.dto.transaction.TransactionCreateRequest;
import org.example.doansummer2026.dto.transaction.TransactionResponse;
import org.example.doansummer2026.dto.transaction.TransactionUpdateRequest;
import org.example.doansummer2026.exception.BadRequestException;
import org.example.doansummer2026.exception.ConflictException;
import org.example.doansummer2026.exception.ResourceNotFoundException;
import org.example.doansummer2026.model.Invoice;
import org.example.doansummer2026.enums.InvoiceStatus;
import org.example.doansummer2026.model.StaffInfo;
import org.example.doansummer2026.model.Transaction;
import org.example.doansummer2026.enums.TransactionStatus;
import org.example.doansummer2026.repository.InvoiceRepository;
import org.example.doansummer2026.repository.StaffInfoRepository;
import org.example.doansummer2026.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.example.doansummer2026.service.interfaces.TransactionServiceInterface;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
@RequiredArgsConstructor
public class TransactionService implements TransactionServiceInterface {

    private final TransactionRepository repo;
    private final InvoiceRepository invoiceRepo;
    private final InvoiceService invoiceService;
    private final StaffInfoRepository staffRepo;

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> search(UUID invoiceId, TransactionStatus status,
                                                     LocalDateTime from, LocalDateTime to,
                                                     Pageable pageable) {
        Page<Transaction> page = repo.search(invoiceId, status, from, to, pageable);
        return PageResponse.from(page, TransactionResponse::from);
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(UUID id) {
        return TransactionResponse.from(findById(id));
    }

    public TransactionResponse create(TransactionCreateRequest req) {
        Invoice invoice = invoiceRepo.findById(req.invoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Hoa don khong ton tai: " + req.invoiceId()));
        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new ConflictException("Chi tao giao dich cho hoa don PENDING; hien tai: "
                    + invoice.getStatus());
        }
        StaffInfo receivedBy = null;
        if (req.receivedById() != null) {
            receivedBy = staffRepo.findById(req.receivedById())
                    .orElseThrow(() -> new ResourceNotFoundException("Nhan vien khong ton tai: " + req.receivedById()));
        }
        Transaction t = Transaction.builder()
                .invoice(invoice)
                .transactionCode(generateTransactionCode(invoice.getInvoiceCode()))
                .amount(req.amount())
                .paymentMethod(req.paymentMethod())
                .status(TransactionStatus.PENDING)
                .gatewayReference(req.gatewayReference())
                .note(req.note())
                .receivedBy(receivedBy)
                .build();
        return TransactionResponse.from(repo.save(t));
    }

    public TransactionResponse update(UUID id, TransactionUpdateRequest req) {
        Transaction t = findById(id);
        if (req.status() != null) {
            validateStatusTransition(t.getStatus(), req.status());
            t.setStatus(req.status());
            if (req.status() == TransactionStatus.SUCCESS && t.getPaidAt() == null) {
                t.setPaidAt(LocalDateTime.now());
            }
        }
        if (req.paidAt() != null) t.setPaidAt(req.paidAt());
        if (req.gatewayReference() != null) t.setGatewayReference(req.gatewayReference());
        if (req.note() != null) t.setNote(req.note());
        Transaction saved = repo.save(t);
        invoiceService.recalculatePaidAmount(saved.getInvoice().getInvoiceId());
        return TransactionResponse.from(saved);
    }

    public TransactionResponse confirm(UUID id) {
        return update(id, new TransactionUpdateRequest(TransactionStatus.SUCCESS, null, null, null));
    }

    public TransactionResponse fail(UUID id) {
        return update(id, new TransactionUpdateRequest(TransactionStatus.FAILED, null, null, null));
    }

    
    public void delete(UUID id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Giao dich khong ton tai: " + id);
        }
        UUID invoiceId = findById(id).getInvoice().getInvoiceId();
        repo.deleteById(id);
        invoiceService.recalculatePaidAmount(invoiceId);
    }

    public Transaction findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Giao dich khong ton tai: " + id));
    }

    private void validateStatusTransition(TransactionStatus from, TransactionStatus to) {
        boolean ok = switch (from) {
            case PENDING -> to == TransactionStatus.SUCCESS || to == TransactionStatus.FAILED
                    || to == TransactionStatus.CANCELLED;
            default -> false;
        };
        if (!ok) throw new BadRequestException("Khong the chuyen trang thai tu " + from + " sang " + to);
    }

    private String generateTransactionCode(String invoiceCode) {
        String prefix = "TXN-" + invoiceCode + "-";
        for (int attempt = 0; attempt < 3; attempt++) {
            String suffix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"))
                    + "-" + String.format("%04X", ThreadLocalRandom.current().nextInt(0, 0xFFFF));
            String code = prefix + suffix;
            if (!repo.existsByTransactionCode(code)) return code;
        }
        throw new ConflictException("Khong the sinh transaction code");
    }
}
