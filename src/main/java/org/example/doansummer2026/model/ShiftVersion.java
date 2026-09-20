package org.example.doansummer2026.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.doansummer2026.common.BaseEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "shift_version", indexes = {
        @Index(name = "idx_shift_version_effective", columnList = "shift_id,effective_from,effective_to")
})
@SQLDelete(sql = "UPDATE shift_version SET deleted = true WHERE shift_version_id = ?")
@SQLRestriction("deleted = false")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ShiftVersion extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "shift_version_id")
    private UUID shiftVersionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shift_id", nullable = false)
    private ShiftConfig shift;

    @Column(name = "start_time", nullable = false)
    @JdbcTypeCode(SqlTypes.LOCAL_TIME)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    @JdbcTypeCode(SqlTypes.LOCAL_TIME)
    private LocalTime endTime;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "change_reason", nullable = false, length = 500)
    private String changeReason;

}
