package org.example.doansummer2026.dto.staff;

import jakarta.validation.constraints.Size;

/** Cac truong nghe nghiep nhan vien duoc phep tu cap nhat. */
public record StaffProfessionalUpdateRequest(
        @Size(max = 100) String highestDegree,
        @Size(max = 200) String university
) {}
