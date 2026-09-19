package com.example.hospital_management_system.exception;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    // The three handlers below only became reachable once actual HTTP requests entered the
    // picture via controllers - none of them could be thrown by a direct service call. Without
    // these, each would fall through to Spring Boot's default /error handler and return its
    // generic error body instead of our ErrorResponse shape.

    /** Malformed JSON, or a request body that doesn't match the target DTO's shape at all. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleUnreadableBody(HttpMessageNotReadableException ex) {
        return new ErrorResponse("Malformed request body");
    }

    /** E.g. GET /api/patients/abc, where {id} can't be parsed as a Long. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return new ErrorResponse("Invalid value for parameter '" + ex.getName() + "': " + ex.getValue());
    }

    /** E.g. GET /api/doctors/{id}/availability without the required from/to query params. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMissingParam(MissingServletRequestParameterException ex) {
        return new ErrorResponse("Missing required parameter: " + ex.getParameterName());
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicateUsername(DuplicateUsernameException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    /**
     * Covers AuthenticationManager.authenticate() failures in AuthService.login() - wrong
     * username or password both surface as BadCredentialsException (a subtype of this), never
     * distinguished, so a caller can't use the error to enumerate which usernames exist. This
     * handler catches it because login() calls authenticate() directly inside a normal service
     * call, which DOES flow through ordinary Spring MVC exception handling. Contrast with
     * RestAuthenticationEntryPoint, which handles the OTHER 401 case - a request with no/invalid
     * JWT hitting a protected URL, rejected at the security-filter level before any controller
     * runs, which never reaches this class at all.
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthenticationFailure(AuthenticationException ex) {
        return new ErrorResponse("Invalid username or password");
    }

    /**
     * Covers @PreAuthorize denials (e.g. a PATIENT calling DELETE /api/appointments/{id} on
     * someone else's appointment) - thrown from inside the controller invocation, so it flows
     * through normal Spring MVC exception handling same as AuthenticationException above.
     * Contrast with RestAccessDeniedHandler, which handles the URL-pattern-rule case (e.g. a
     * PATIENT calling POST /api/doctors), rejected at the filter level before reaching a
     * controller, and therefore also never reaching this class.
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDenied(AccessDeniedException ex) {
        return new ErrorResponse("Access denied");
    }
}
