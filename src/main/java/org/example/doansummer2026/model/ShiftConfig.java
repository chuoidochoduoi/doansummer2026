package org.example.doansummer2026.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.doansummer2026.common.BaseEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Entity
@Table(name = "shift_config")
@SQLDelete(sql = "UPDATE shift_config SET deleted = true WHERE shift_id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "shift_id")
    private UUID shiftId;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String name; // VD: "Ca Sáng"

    @NotBlank
    @Column(name = "start_time", nullable = false, length = 10)
    private String startTime; // VD: "07:30"

    @NotBlank
    @Column(name = "end_time", nullable = false, length = 10)
    private String endTime; // VD: "11:30"

    @NotNull
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

}
