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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.doansummer2026.common.BaseEntity;
import org.example.doansummer2026.enums.AppointmentStatus;
import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.enums.TimeSlot;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Lich hen kham benh (booking truoc khi check-in).
 * - 1-1 voi CustomerVisit (chi co khi khach hang da check-in).
 */
@Entity
@Table(name = "appointment")
@SQLDelete(sql = "UPDATE appointment SET deleted = true WHERE appointment_id = ?")
@SQLRestriction("deleted = false")
@NamedEntityGraph(
    name = "Appointment.withDetails",
    attributeNodes = {
        @NamedAttributeNode("customer"),
        @NamedAttributeNode(value = "services", subgraph = "service-dept")
    },
    subgraphs = {
        @NamedSubgraph(
            name = "service-dept",
            attributeNodes = {
                @NamedAttributeNode("department")
            }
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "appointment_id")
    private UUID appointmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Profile customer;

    @NotNull
    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.PENDING;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    /** Quan he 1-1 voi CustomerVisit - chi co khi da check-in. */
    @OneToOne(mappedBy = "appointment")
    private CustomerVisit visit;

    /** Cho phep dat lich khong can dang nhap. */
    @Column(name = "is_guest")
    @Builder.Default
    private Boolean isGuest = false;

    @Size(max = 100)
    @Column(name = "guest_full_name", length = 100)
    private String guestFullName;

    @Size(max = 15)
    @Column(name = "guest_phone", length = 15)
    private String guestPhone;

    @Column(name = "guest_age")
    private Integer guestAge;

    @Enumerated(EnumType.STRING)
    @Column(name = "guest_gender", length = 10)
    private Gender guestGender;

    @Size(max = 255)
    @Column(name = "guest_address")
    private String guestAddress;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_slot", length = 10)
    private TimeSlot timeSlot;

    /** Cac dich vu kham da chon khi dat lich (co the thay doi khi check-in). */
    @ManyToMany
    @JoinTable(
            name = "appointment_services",
            joinColumns = @JoinColumn(name = "appointment_id"),
            inverseJoinColumns = @JoinColumn(name = "service_id")
    )
    @Builder.Default
    private Set<MedicalService> services = new HashSet<>();
}