package com.example.hospital_management_system.exception;

import java.util.List;

public record ValidationErrorResponse(String message, List<FieldErrorDetail> errors) {}
