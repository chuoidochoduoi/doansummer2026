package vn.edu.fpt.cares.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.edu.fpt.cares.common.BaseEntity;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "clinic_information")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClinicInformation extends BaseEntity {
    @Id
    @Column(name = "clinic_information_id")
    private UUID clinicInformationId;

    @Column(name = "clinic_name", nullable = false, length = 150)
    private String clinicName;

    @Column(name = "legal_name", nullable = false, length = 200)
    private String legalName;

    @Column(name = "tax_code", nullable = false, length = 14)
    private String taxCode;

    @Column(name = "operating_license", length = 100)
    private String operatingLicense;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "support_email", nullable = false, length = 150)
    private String supportEmail;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(nullable = false, length = 300)
    private String address;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "facebook_url", length = 500)
    private String facebookUrl;

    @Column(name = "youtube_url", length = 500)
    private String youtubeUrl;

    @Column(name = "zalo_url", length = 500)
    private String zaloUrl;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;
}
