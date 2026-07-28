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
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.doansummer2026.common.BaseEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;
import org.example.doansummer2026.enums.DepartmentStatus;
import org.example.doansummer2026.enums.DepartmentType;

@Entity
@Table(name = "department")
@SQLDelete(sql = "UPDATE department SET deleted = true WHERE department_id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "department_id")
    private UUID departmentId;

    /** Ma phong (R-101, R-102...) */
    @NotBlank
    @Size(max = 20)
    @Column(name = "room_code", nullable = false, unique = true, length = 20)
    private String roomCode;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, unique = true, length = 150)
    private String name;

    /** Trang thai phong */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DepartmentStatus status = DepartmentStatus.AVAILABLE;

    /** Loai phong/khoa */
    @Enumerated(EnumType.STRING)
    @Column(name = "department_type", nullable = false, length = 20)
    @Builder.Default
    private DepartmentType departmentType = DepartmentType.EXAMINATION;

    /** Ghi chu/thiet bi (mo ta them) */
    @Size(max = 500)
    @Column(length = 500)
    private String description;

    /** Bac si phu trach - quan he ManyToOne toi StaffInfo */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "head_doctor_id")
    private StaffInfo headDoctor;
}



