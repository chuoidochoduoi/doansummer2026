package org.example.doansummer2026.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Danh muc dich vu y te (co the phan cap parent/child).
 * Vi du:
 *   Kham benh (cha)
 *     - Kham noi tong quat (con)
 *     - Kham nhi (con)
 *   Xet nghiem (cha)
 *     - Xet nghiem mau (con)
 *     - Xet nghiem nuoc tieu (con)
 */
@Entity
@Table(name = "service_category")
@SQLDelete(sql = "UPDATE service_category SET deleted = true WHERE category_id = ?")
@SQLRestriction("deleted = false")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceCategory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "category_id")
    private UUID categoryId;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Size(max = 500)
    @Column(length = 500)
    private String description;

    /** Nullable: neu null thi day la category goc. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private ServiceCategory parentCategory;

    /** Sub-categories - LAZY, KHONG cascade xoa cung (su dung soft-delete). */
    @OneToMany(mappedBy = "parentCategory", fetch = FetchType.LAZY)
    @OrderBy("name ASC")
    @Builder.Default
    private List<ServiceCategory> subCategories = new ArrayList<>();

}



