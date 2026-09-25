package vn.edu.fpt.cares.dto.medicalrecord;

import vn.edu.fpt.cares.model.MedicineCatalog;
import java.util.UUID;

public record MedicineCatalogResponse(
        UUID medicineId, String medicineCode, String name, String activeIngredient,
        String defaultUnit, String defaultUsage, Integer defaultFrequencyPerDay
) {
    public static MedicineCatalogResponse from(MedicineCatalog medicine) {
        return new MedicineCatalogResponse(medicine.getMedicineId(), medicine.getMedicineCode(), medicine.getName(),
                medicine.getActiveIngredient(), medicine.getDefaultUnit(), medicine.getDefaultUsage(),
                medicine.getDefaultFrequencyPerDay());
    }
}
