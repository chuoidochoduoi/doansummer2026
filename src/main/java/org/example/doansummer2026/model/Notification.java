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
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.doansummer2026.common.BaseEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;
import org.example.doansummer2026.enums.NotificationChannel;
import org.example.doansummer2026.enums.NotificationStatus;
import org.example.doansummer2026.enums.NotificationType;

/**
 * Thong bao gui den user (benh nhan/nhan vien).
 * - recipient: Profile nguoi nhan (FK bat buoc).
 * - type: NotificationType.
 * - channel: NotificationChannel (IN_APP/EMAIL/SMS/PUSH).
 * - title + content: noi dung.
 * - relatedEntity: optional - loai entity lien quan (VD: "Appointment"), relatedEntityId UUID tuong ung.
 *   De navigate UI notification -> chi tiet entity.
 * - status: PENDING/SENT/FAILED/READ.
 */
@Entity
@Table(name = "notification")
@SQLDelete(sql = "UPDATE notification SET deleted = true WHERE notification_id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notification_id")
    private UUID notificationId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Profile recipient;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 40)
    private NotificationType notificationType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationChannel channel;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Size(max = 50)
    @Column(name = "related_entity", length = 50)
    private String relatedEntity;

    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;
}
