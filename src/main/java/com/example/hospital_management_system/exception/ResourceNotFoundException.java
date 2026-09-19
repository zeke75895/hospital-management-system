package com.example.hospital_management_system.exception;

// Extends RuntimeException, not Exception: Spring's default @Transactional rollback rule only
// fires for unchecked exceptions (RuntimeException/Error), not checked ones, unless you add
// rollbackFor to every @Transactional annotation. Every custom exception in this package is
// unchecked on purpose so the default rule just works.
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
