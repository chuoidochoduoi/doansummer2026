package org.example.doansummer2026.model;
import jakarta.persistence.*;
import lombok.*;
import org.example.doansummer2026.common.BaseEntity;
import org.example.doansummer2026.enums.AttendanceStatus;
import java.time.LocalDateTime;
import java.util.UUID;
@Entity @Table(name="staff_attendance",uniqueConstraints=@UniqueConstraint(name="uk_attendance_schedule",columnNames="schedule_id"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class StaffAttendance extends BaseEntity {
 @Id @GeneratedValue(strategy=GenerationType.UUID) @Column(name="attendance_id") private UUID attendanceId;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="schedule_id",nullable=false) private StaffSchedule schedule;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="staff_id",nullable=false) private StaffInfo staff;
 @Column(name="check_in_at") private LocalDateTime checkInAt;
 @Column(name="check_out_at") private LocalDateTime checkOutAt;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private AttendanceStatus status;
 @Column(name="check_in_ip",length=64) private String checkInIp;
 @Column(name="check_out_ip",length=64) private String checkOutIp;
 @Column(name="device_info",length=500) private String deviceInfo;
}
