package org.example.doansummer2026.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Helper chuan hoa response:
 * - 201 Created + Location header (cho POST tao resource don le).
 * - 200 OK cho login/register/refresh/generate (khong co Location don le).
 * - 204 No Content cho PUT/DELETE/PATCH khong tra body.
 */
public final class RestResponses {

    private RestResponses() {}

    /** POST tao resource don le -> 201 Created + Location. */
    public static <T> ResponseEntity<T> created(URI location, T body) {
        return ResponseEntity.created(location).body(body);
    }

    /**
     * POST tao resource don le, tu dong sinh Location theo path template.
     * Vi du: created("/api/v1/staff/{id}", id, body).
     */
    public static <T> ResponseEntity<T> created(String pathTemplate, UUID id, T body) {
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path(pathTemplate)
                .buildAndExpand(id)
                .toUri();
        return created(location, body);
    }

    /** Overload cho khi id la String (VD: ICD-10 code "N20.0"). */
    public static <T> ResponseEntity<T> created(String pathTemplate, String id, T body) {
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path(pathTemplate)
                .buildAndExpand(id)
                .toUri();
        return created(location, body);
    }

    /** POST tac vu (login/register/refresh/generate) -> 200 OK. */
    public static <T> ResponseEntity<T> ok(T body) {
        return ResponseEntity.ok(body);
    }

    /** POST/DELETE -> 204 No Content. */
    public static ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }

    /**
     * Chay supplier; neu throw RuntimeException khong phai ResourceNotFoundException thi bao loi.
     * Hien chua can thiet, de mo rong sau.
     */
    public static <T> ResponseEntity<T> wrap(Supplier<ResponseEntity<T>> supplier) {
        return supplier.get();
    }

    public static HttpStatus statusCreated() {
        return HttpStatus.CREATED;
    }
}



