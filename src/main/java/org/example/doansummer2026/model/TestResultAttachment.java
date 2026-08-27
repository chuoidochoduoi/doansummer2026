package org.example.doansummer2026.model;

import jakarta.persistence.*;
import lombok.*;
import org.example.doansummer2026.common.BaseEntity;

import java.util.UUID;

@Entity
@Table(name = "test_result_attachment")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TestResultAttachment extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "attachment_id")
    private UUID attachmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revision_id", nullable = false)
    private TestResultRevision revision;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
