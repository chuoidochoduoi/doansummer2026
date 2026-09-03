package org.example.doansummer2026.dto.medicalrecord;

import org.example.doansummer2026.enums.AllergyStatus;
import org.example.doansummer2026.model.Profile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public record PatientAllergyResponse(AllergyStatus status, List<String> items) {

    public static PatientAllergyResponse from(Profile profile) {
        String stored = profile == null ? null : profile.getAllergies();
        if (stored == null) return new PatientAllergyResponse(AllergyStatus.UNVERIFIED, List.of());
        List<String> items = normalize(List.of(stored.split("[;\\r\\n]+")));
        return items.isEmpty()
                ? new PatientAllergyResponse(AllergyStatus.NONE_REPORTED, List.of())
                : new PatientAllergyResponse(AllergyStatus.REPORTED, items);
    }

    public static List<String> normalize(List<String> values) {
        if (values == null) return List.of();
        LinkedHashMap<String, String> unique = new LinkedHashMap<>();
        for (String value : values) {
            if (value == null) continue;
            String clean = value.trim().replaceAll("\\s+", " ");
            if (!clean.isBlank()) unique.putIfAbsent(clean.toLowerCase(Locale.ROOT), clean);
        }
        return new ArrayList<>(unique.values());
    }
}
