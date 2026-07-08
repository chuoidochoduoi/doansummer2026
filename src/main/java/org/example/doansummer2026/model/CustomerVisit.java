package org.example.doansummer2026.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.doansummer2026.common.BaseEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.example.doansummer2026.enums.VisitStatus;

/**
 * Luot kham khach hang (khi khach hang da check-in).
 * - Tao tu Appointment (khi check-in) hoac tao moi cho khach vang lai.
 * - 1-1 voi MedicalRecord (owning o MedicalRecord).
 * - 1-n voi QueueTicket (mot visit co the xep hang o nhieu khoa xet nghiem/CDHA).
 */
@Entity
@Table(name = "customer_visit")
@SQLDelete(sql = "UPDATE customer_visit SET deleted = true WHERE visit_id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerVisit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "visit_id")
    private UUID visitId;

    /** Nullable cho khach vang lai (guest appointment). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Profile customer;

    /** Nullable - khach vang lai khong co appointment truoc. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @NotNull
    @Column(name = "check_in_time", nullable = false)
    @Builder.Default
    private LocalDateTime checkInTime = LocalDateTime.now();

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VisitStatus status = VisitStatus.CHECKED_IN;

    @OneToMany(mappedBy = "visit", fetch = FetchType.LAZY)
    @Builder.Default
    private List<QueueTicket> queueTickets = new ArrayList<>();
}