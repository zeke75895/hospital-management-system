package com.example.hospital_management_system.exception;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
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

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ResourceNotFoundException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    // DoubleBookingException: the pessimistic-locked conflict check in AppointmentService found
    // an existing BOOKED appointment. DoctorNotAvailableException: scheduledAt doesn't fall
    // inside any of the doctor's Schedule windows. InvalidAppointmentStateException: a
    // cancel/complete was attempted on an appointment whose current status or timing doesn't
    // allow it. All three are "this can't happen given the current state" - 409, not 400 (the
    // request is well-formed, it just conflicts with server-side state).
    @ExceptionHandler({
        DoubleBookingException.class,
        DoctorNotAvailableException.class,
        InvalidAppointmentStateException.class
    })
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(RuntimeException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    // Thrown by Hibernate when @Version detects a stale write - e.g. one request cancels an
    // appointment while another concurrently completes the same one. This is the 409 path for
    // the optimistic-locking half of the concurrency design (see Appointment.version and
    // AppointmentService.cancelAppointment/completeAppointment).
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        return new ErrorResponse("This appointment was modified by another request; please retry.");
    }
}
