package org.example.doansummer2026.model;
import jakarta.persistence.*;
import lombok.*;
import org.example.doansummer2026.common.BaseEntity;
import org.example.doansummer2026.enums.AttendanceAdjustmentStatus;
import java.time.LocalDateTime;
import java.util.UUID;
@Entity @Table(name="attendance_adjustment") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AttendanceAdjustment extends BaseEntity {
 @Id @GeneratedValue(strategy=GenerationType.UUID) @Column(name="adjustment_id") private UUID adjustmentId;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="attendance_id",nullable=false) private StaffAttendance attendance;
 @Column(nullable=false,length=1000) private String reason;
 @Column(name="requested_check_in") private LocalDateTime requestedCheckIn;
 @Column(name="requested_check_out") private LocalDateTime requestedCheckOut;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) @Builder.Default private AttendanceAdjustmentStatus status=AttendanceAdjustmentStatus.PENDING;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reviewed_by") private StaffInfo reviewedBy;
 @Column(name="reviewed_at") private LocalDateTime reviewedAt;
 @Column(name="review_note",length=1000) private String reviewNote;
}
