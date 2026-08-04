package org.example.doansummer2026.model;
import jakarta.persistence.*;
import lombok.*;
import org.example.doansummer2026.common.BaseEntity;
import java.time.LocalDateTime;
import java.util.UUID;
@Entity @Table(name="attendance_qr_token") @Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AttendanceQrToken extends BaseEntity {
 @Id @GeneratedValue(strategy=GenerationType.UUID) @Column(name="token_id") private UUID tokenId;
 @Column(name="token_hash",nullable=false,unique=true,length=64) private String tokenHash;
 @Column(name="expires_at",nullable=false) private LocalDateTime expiresAt;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="created_by",nullable=false) private StaffInfo createdBy;
 @Column(nullable=false) @Builder.Default private Boolean active=true;
}
