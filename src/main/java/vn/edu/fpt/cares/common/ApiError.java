package vn.edu.fpt.cares.common;

import java.time.Instant;
import java.util.List;

/**
 * Cau truc JSON tra ve khi loi (validation, not found, conflict, ...).
 * Spring se tu serialize qua Jackson.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldError> errors
) {
    public record FieldError(String field, String message) {}
}



