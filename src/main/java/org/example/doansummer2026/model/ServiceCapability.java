package org.example.doansummer2026.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.example.doansummer2026.common.BaseEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import java.util.UUID;

@Entity
@Table(name = "service_capability")
@SQLDelete(sql = "UPDATE service_capability SET deleted = true WHERE capability_id = ?")
@SQLRestriction("deleted = false")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceCapability extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "capability_id")
    private UUID capabilityId;

    @NotBlank @Size(max = 30)
    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @NotBlank @Size(max = 150)
    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Size(max = 500)
    @Column(length = 500)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;
}
