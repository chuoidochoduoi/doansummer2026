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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.doansummer2026.common.BaseEntity;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.util.UUID;
import org.example.doansummer2026.enums.Gender;
import org.example.doansummer2026.enums.BloodType;

/**
 * Ho so ca nhan: thong tin lien lac, nhom mau, ngay sinh, ...
 * - Moi Account co 0..1 Profile (PATIENT khong bat buoc, RECEPTIONIST/CASHIER/NURSE/DOCTOR bat buoc khi tao qua StaffService).
 * - BHQT/di ung se tach bang rieng o buoc sau.
 */
@Entity
@Table(name = "profile")
@SQLDelete(sql = "UPDATE profile SET deleted = true WHERE profile_id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "profile_id")
    private UUID profileId;

    /** Owning side cua quan he 1-1 voi Account (nullable cho khach vang lai). */
    @OneToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "account_id")
    private Account account;
    @Size(max = 100)
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Past
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @NotBlank
    @Pattern(regexp = "^(\\+84|0)\\d{9,10}$", message = "So dien thoai khong hop le (VN)")
    @Column(nullable = false, unique = true, length = 15)
    private String phone;

    @Email
    @Column(unique = true)
    private String email;

    @Size(max = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_type", length = 15)
    private BloodType bloodType;

    /** So the bao hiem y te. */
    @Size(max = 50)
    @Column(name = "insurance_id", length = 50)
    private String insuranceId;

    /** Chieu cao (cm). */
    private Integer height;

    /** Can nang (kg). */
    private Integer weight;

    /** Danh sach di ung (JSON array). */
    @Column(columnDefinition = "TEXT")
    private String allergies;
}




