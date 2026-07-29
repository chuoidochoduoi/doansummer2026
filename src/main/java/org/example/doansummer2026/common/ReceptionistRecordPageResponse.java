package org.example.doansummer2026.common;

import java.util.List;

/**
 * Response format cho API le tan /api/receptionist/records.
 * Frontend mong doi format: { items: [...], total: 100, page: 1 }
 */
public record ReceptionistRecordPageResponse<T>(
        List<T> items,
        long total,
        int page
) {
    public static <T> ReceptionistRecordPageResponse<T> from(PageResponse<T> pageResponse) {
        return new ReceptionistRecordPageResponse<>(
                pageResponse.content(),
                pageResponse.totalElements(),
                pageResponse.page() + 1 // Convert 0-based to 1-based
        );
    }
}