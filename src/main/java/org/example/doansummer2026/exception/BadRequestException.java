package org.example.doansummer2026.exception;

/**
 * Loi 400: nghiep vu khong hop le (khac voi validation annotation).
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}



