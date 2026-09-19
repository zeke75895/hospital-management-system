package com.example.hospital_management_system.exception;

public class DuplicateUsernameException extends RuntimeException {
    public DuplicateUsernameException(String message) {
        super(message);
    }
}
