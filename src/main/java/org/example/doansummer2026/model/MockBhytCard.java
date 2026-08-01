package org.example.doansummer2026.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.doansummer2026.common.BaseEntity;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "mock_bhyt_card")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockBhytCard extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "card_id")
    private UUID cardId;

    @NotNull
    @Column(name = "card_number", unique = true, length = 20, nullable = false)
    private String cardNumber;

    @NotNull
    @Column(name = "full_name", length = 100, nullable = false)
    private String fullName;
    
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_id", nullable = false)
    private Insurance insurance;

    @NotNull
    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @NotNull
    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;
}
