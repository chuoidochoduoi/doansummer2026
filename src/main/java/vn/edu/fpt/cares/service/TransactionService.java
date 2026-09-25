package vn.edu.fpt.cares.service;

import lombok.RequiredArgsConstructor;
import vn.edu.fpt.cares.common.PageResponse;
import vn.edu.fpt.cares.dto.transaction.TransactionCreateRequest;
import vn.edu.fpt.cares.dto.transaction.TransactionResponse;
import vn.edu.fpt.cares.dto.transaction.TransactionUpdateRequest;
import vn.edu.fpt.cares.exception.BadRequestException;
import vn.edu.fpt.cares.exception.ConflictException;
import vn.edu.fpt.cares.exception.ResourceNotFoundException;
import vn.edu.fpt.cares.model.Invoice;
import vn.edu.fpt.cares.enums.InvoiceStatus;
import vn.edu.fpt.cares.model.StaffInfo;
import vn.edu.fpt.cares.model.Transaction;
import vn.edu.fpt.cares.enums.TransactionStatus;
import vn.edu.fpt.cares.repository.InvoiceRepository;
import vn.edu.fpt.cares.repository.StaffInfoRepository;
import vn.edu.fpt.cares.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.edu.fpt.cares.service.interfaces.TransactionServiceInterface;
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
        Invoice invoice = invoiceRepo.findByIdForUpdate(req.invoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Hóa đơn không tồn tại: " + req.invoiceId()));
        if (invoice.getStatus() != InvoiceStatus.PENDING) {
            throw new ConflictException("Chỉ có thể tạo giao dịch cho hóa đơn đang chờ thanh toán; trạng thái hiện tại: "
                    + invoice.getStatus());
        }
        java.math.BigDecimal successfulPaid = successfulPaidAmount(invoice.getInvoiceId(), null);
        java.math.BigDecimal remaining = invoice.getTotalAmount().subtract(successfulPaid);
        if (req.amount().compareTo(java.math.BigDecimal.ZERO) <= 0
                || req.amount().compareTo(remaining) > 0) {
            throw new BadRequestException("Số tiền giao dịch vượt quá số tiền còn phải thanh toán: " + remaining);
        }
        StaffInfo receivedBy = null;
        if (req.receivedById() != null) {
            receivedBy = staffRepo.findById(req.receivedById())
                    .orElseThrow(() -> new ResourceNotFoundException("Nhân viên không tồn tại: " + req.receivedById()));
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
        Transaction t = repo.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Giao dịch không tồn tại: " + id));
        Invoice lockedInvoice = invoiceRepo.findByIdForUpdate(t.getInvoice().getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Hóa đơn của giao dịch không tồn tại"));
        if (req.status() != null) {
            validateStatusTransition(t.getStatus(), req.status());
            if (req.status() == TransactionStatus.SUCCESS) {
                java.math.BigDecimal successfulPaid = successfulPaidAmount(
                        lockedInvoice.getInvoiceId(), t.getTransactionId());
                java.math.BigDecimal remaining = lockedInvoice.getTotalAmount().subtract(successfulPaid);
                if (t.getAmount().compareTo(remaining) > 0) {
                    throw new ConflictException(
                            "Không thể xác nhận giao dịch vì số tiền vượt số dư hóa đơn: " + remaining);
                }
            }
            t.setStatus(req.status());
            if (req.status() == TransactionStatus.SUCCESS && t.getPaidAt() == null) {
                t.setPaidAt(LocalDateTime.now());
            }
        }
        // paidAt là dấu thời gian hệ thống, không nhận giá trị tùy ý từ frontend.
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
        findById(id);
        throw new ConflictException("Không thể xóa giao dịch thanh toán. Hãy chuyển giao dịch chưa thành công sang Thất bại hoặc Đã hủy");
    }

    public Transaction findById(UUID id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Giao dịch không tồn tại: " + id));
    }

    private void validateStatusTransition(TransactionStatus from, TransactionStatus to) {
        boolean ok = switch (from) {
            case PENDING -> to == TransactionStatus.SUCCESS || to == TransactionStatus.FAILED
                    || to == TransactionStatus.CANCELLED;
            default -> false;
        };
        if (!ok) throw new BadRequestException("Không thể chuyển trạng thái từ " + from + " sang " + to);
    }

    private java.math.BigDecimal successfulPaidAmount(UUID invoiceId, UUID excludedTransactionId) {
        return repo.findByInvoice_InvoiceId(invoiceId).stream()
                .filter(transaction -> transaction.getStatus() == TransactionStatus.SUCCESS)
                .filter(transaction -> excludedTransactionId == null
                        || !excludedTransactionId.equals(transaction.getTransactionId()))
                .map(Transaction::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }

    private String generateTransactionCode(String invoiceCode) {
        String prefix = "TXN-" + invoiceCode + "-";
        for (int attempt = 0; attempt < 3; attempt++) {
            String suffix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"))
                    + "-" + String.format("%04X", ThreadLocalRandom.current().nextInt(0, 0xFFFF));
            String code = prefix + suffix;
            if (!repo.existsByTransactionCode(code)) return code;
        }
        throw new ConflictException("Không thể tạo mã giao dịch");
    }
}




