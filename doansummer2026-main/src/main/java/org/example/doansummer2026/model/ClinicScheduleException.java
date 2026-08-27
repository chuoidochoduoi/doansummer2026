package org.example.doansummer2026.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.doansummer2026.common.BaseEntity;
import org.example.doansummer2026.enums.ClinicScheduleExceptionType;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "clinic_schedule_exception", indexes = {
        @Index(name = "idx_clinic_exception_date", columnList = "work_date")
})
@SQLDelete(sql = "UPDATE clinic_schedule_exception SET deleted = true WHERE exception_id = ?")
@SQLRestriction("deleted = false")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClinicScheduleException extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "exception_id")
    private UUID exceptionId;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shift_id")
    private ShiftConfig shift;

    @Enumerated(EnumType.STRING)
    @Column(name = "exception_type", nullable = false, length = 30)
    private ClinicScheduleExceptionType type;

    @Column(name = "special_start_time")
    @JdbcTypeCode(SqlTypes.LOCAL_TIME)
    private LocalTime specialStartTime;

    @Column(name = "special_end_time")
    @JdbcTypeCode(SqlTypes.LOCAL_TIME)
    private LocalTime specialEndTime;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "created_by")
    private UUID createdBy;
}
