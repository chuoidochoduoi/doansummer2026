package vn.edu.fpt.cares.dto.specialization;

import vn.edu.fpt.cares.model.Specialization;

import java.util.UUID;

public record SpecializationResponse(
        UUID specializationId,
        String name,
        String description,
        Boolean active
) {
    public static SpecializationResponse from(Specialization s) {
        return new SpecializationResponse(s.getSpecializationId(), s.getName(), s.getDescription(),
                !Boolean.FALSE.equals(s.getActive()));
    }
}



