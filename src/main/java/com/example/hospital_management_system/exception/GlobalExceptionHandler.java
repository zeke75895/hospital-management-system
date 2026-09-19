package com.example.hospital_management_system.exception;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles @Valid failures on @RequestBody DTOs. Field-level constraints (@NotBlank, @Email,
     * ...) surface as FieldErrors, each tied to one property. Class-level constraints
     * (@ValidAppointmentSlot, the @AssertTrue cross-field checks on ScheduleRequest) have no
     * single field to attach to and surface as ObjectErrors instead. Both are merged into the
     * response here — mapping only getFieldErrors() would silently drop every class-level
     * validation message.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleValidationErrors(MethodArgumentNotValidException ex) {
        List<FieldErrorDetail> details = new ArrayList<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.add(new FieldErrorDetail(fieldError.getField(), fieldError.getDefaultMessage()));
        }
        for (ObjectError globalError : ex.getBindingResult().getGlobalErrors()) {
            details.add(new FieldErrorDetail(globalError.getObjectName(), globalError.getDefaultMessage()));
        }

        return new ValidationErrorResponse("Validation failed", details);
    }
}
