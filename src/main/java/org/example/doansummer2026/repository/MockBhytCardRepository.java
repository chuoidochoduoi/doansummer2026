package org.example.doansummer2026.repository;

import org.example.doansummer2026.model.MockBhytCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MockBhytCardRepository extends JpaRepository<MockBhytCard, UUID> {
    Optional<MockBhytCard> findByCardNumberAndDeletedFalse(String cardNumber);
}
