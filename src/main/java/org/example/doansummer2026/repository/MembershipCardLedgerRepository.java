package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.MembershipCardLedger;
import org.example.doansummer2026.enums.MembershipLedgerType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface MembershipCardLedgerRepository extends JpaRepository<MembershipCardLedger, UUID> {
    Optional<MembershipCardLedger> findByIdempotencyKey(String key);
    Optional<MembershipCardLedger> findByPaymentTransaction_TransactionId(UUID transactionId);
    boolean existsByReversedLedger_LedgerId(UUID ledgerId);
    Page<MembershipCardLedger> findByCard_CardIdOrderByCreatedAtDesc(UUID cardId, Pageable pageable);
    Page<MembershipCardLedger> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<MembershipCardLedger> findByTypeOrderByCreatedAtDescLedgerIdDesc(MembershipLedgerType type, Pageable pageable);
}
