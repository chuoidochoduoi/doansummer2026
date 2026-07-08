package org.example.doansummer2026.exception;

/**
 * Loi 409: trung unique, da ton tai, ...
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}