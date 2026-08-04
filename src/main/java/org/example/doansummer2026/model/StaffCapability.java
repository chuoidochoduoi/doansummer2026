package org.example.doansummer2026.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.doansummer2026.common.BaseEntity;
import org.example.doansummer2026.enums.StaffCapabilityStatus;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "staff_capability", uniqueConstraints = @UniqueConstraint(columnNames = {"staff_id", "capability_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StaffCapability extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "staff_capability_id")
    private UUID staffCapabilityId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private StaffInfo staff;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "capability_id", nullable = false)
    private ServiceCapability capability;

    @Column(name = "certificate_number", length = 100)
    private String certificateNumber;
    @Column(name = "issued_date")
    private LocalDate issuedDate;
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    @Column(name = "issuing_organization", length = 200)
    private String issuingOrganization;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StaffCapabilityStatus status = StaffCapabilityStatus.ACTIVE;
}
