package com.example.hospital_management_system.controller;

import com.example.hospital_management_system.dto.request.AppointmentRequest;
import com.example.hospital_management_system.dto.response.AppointmentResponse;
import com.example.hospital_management_system.entity.Role;
import com.example.hospital_management_system.security.AppUserDetails;
import com.example.hospital_management_system.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@Tag(name = "Appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    // This is the first place @Valid AppointmentRequest is ever actually triggered - everything
    // built for it earlier (the @Future/@NotBlank field constraints, and the class-level
    // @ValidAppointmentSlot custom validator) was unreachable without a controller invoking Bean
    // Validation on the deserialized request body. It runs before this method body executes at
    // all; AppointmentService.bookAppointment() still repeats its own schedule/conflict checks
    // regardless, since the service can't assume every caller went through this path.
    // #request binds to the @RequestBody parameter by name (same -parameters-flag mechanism as
    // #id elsewhere in this class). A PATIENT may only book for themselves; DOCTOR/ADMIN can book
    // on behalf of any patient (e.g. a phone booking taken by staff).
    @PostMapping
    @PreAuthorize("hasRole('DOCTOR') or hasRole('ADMIN') or #request.patientId == authentication.principal.patientId")
    @Operation(summary = "Book a new appointment (a PATIENT may only book for themselves)")
    @ApiResponse(responseCode = "201", description = "Appointment booked")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "403", description = "A PATIENT tried to book on another patient's behalf")
    @ApiResponse(responseCode = "404", description = "Patient or doctor not found")
    @ApiResponse(responseCode = "409", description = "Doctor unavailable, or already booked at this time")
    public ResponseEntity<AppointmentResponse> bookAppointment(@Valid @RequestBody AppointmentRequest request) {
        AppointmentResponse response = appointmentService.bookAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // Unlike getPatient/getPatientHistory, the id here is an appointmentId, not a patientId - there's
    // no direct value to compare against authentication.principal.patientId, so this needs an actual
    // lookup (does this appointment belong to me?) rather than a plain equality check. That lookup is
    // what AppointmentSecurity.isOwnedByCurrentPatient exists for; see its Javadoc.
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR') or @appointmentSecurity.isOwnedByCurrentPatient(#id, authentication)")
    @Operation(summary = "Cancel a booked appointment (a PATIENT may only cancel their own)")
    @ApiResponse(responseCode = "204", description = "Appointment cancelled")
    @ApiResponse(responseCode = "403", description = "A PATIENT tried to cancel another patient's appointment")
    @ApiResponse(responseCode = "404", description = "Appointment not found")
    @ApiResponse(responseCode = "409", description = "Appointment isn't BOOKED, or has already passed")
    public ResponseEntity<Void> cancelAppointment(@PathVariable Long id) {
        appointmentService.cancelAppointment(id);
        return ResponseEntity.noContent().build();
    }

    // No @PreAuthorize here: this is a pure role check (DOCTOR/ADMIN, no per-resource identity
    // involved), so it's handled entirely by the PATCH /api/appointments/*/complete URL rule in
    // SecurityConfig instead - the simpler, cheaper tool for this particular case.
    @PatchMapping("/{id}/complete")
    @Operation(summary = "Mark a booked appointment as completed (DOCTOR or ADMIN only)")
    @ApiResponse(responseCode = "200", description = "Appointment marked completed")
    @ApiResponse(responseCode = "404", description = "Appointment not found")
    @ApiResponse(responseCode = "409", description = "Appointment isn't BOOKED")
    public AppointmentResponse completeAppointment(@PathVariable Long id) {
        return appointmentService.completeAppointment(id);
    }

    // Ownership here is about SCOPING the result set, not a binary allow/deny - not really a
    // @PreAuthorize case at all. A PATIENT passing someone else's patientId is rejected outright
    // (403, via the same AccessDeniedException GlobalExceptionHandler already maps); a PATIENT
    // omitting patientId gets it silently defaulted to their own, so "my appointments" doesn't
    // require them to already know their own patientId.
    @GetMapping
    @Operation(summary = "Search appointments by doctor, patient, and/or date (all filters optional)")
    @ApiResponse(responseCode = "200", description = "Matching appointments returned")
    @ApiResponse(responseCode = "403", description = "A PATIENT supplied a patientId that isn't their own")
    public List<AppointmentResponse> searchAppointments(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal AppUserDetails principal) {
        Long effectivePatientId = patientId;
        if (principal.getRole() == Role.PATIENT) {
            if (patientId != null && !patientId.equals(principal.getPatientId())) {
                throw new AccessDeniedException("A PATIENT may only search their own appointments");
            }
            effectivePatientId = principal.getPatientId();
        }
        return appointmentService.searchAppointments(doctorId, effectivePatientId, date);
    }
}
