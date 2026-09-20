package com.example.hospital_management_system.controller;

import com.example.hospital_management_system.dto.request.PatientRequest;
import com.example.hospital_management_system.dto.response.AppointmentResponse;
import com.example.hospital_management_system.dto.response.PatientResponse;
import com.example.hospital_management_system.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
@Tag(name = "Patients")
@SecurityRequirement(name = "bearerAuth")
public class PatientController {

    private final PatientService patientService;

    @PostMapping
    @Operation(summary = "Register a new patient")
    @ApiResponse(responseCode = "201", description = "Patient created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    public ResponseEntity<PatientResponse> createPatient(@Valid @RequestBody PatientRequest request) {
        PatientResponse response = patientService.createPatient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // This is exactly the case URL-based rules can't express: SecurityConfig can say "/api/patients/**
    // requires authentication" for the whole path, but it has no way to compare the :id in this
    // specific request against which patient the caller actually is - that comparison needs the
    // resolved path variable and the authenticated principal's data together, which is what
    // @PreAuthorize's SpEL evaluates against. #id binds to the @PathVariable of the same name
    // (available because the compiler retains parameter names - Spring Boot's parent POM enables
    // -parameters by default); authentication.principal is the AppUserDetails set by
    // JwtAuthenticationFilter.
    // No ownership case here - unlike getPatient/getPatientHistory below, this is a plain role
    // check (can this caller browse the whole patient list at all?), so it's @PreAuthorize with a
    // role expression rather than an ownership comparison. It's method-level rather than a
    // SecurityConfig URL rule only because GET /api/patients/{id} (ownership-checked) and GET
    // /api/patients (role-checked) share this same controller and need different logic - keeping
    // both here avoids splitting one controller's authorization across two places.
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    @Operation(summary = "List patients (staff only - a PATIENT has no use for this and no access to it)")
    @ApiResponse(responseCode = "200", description = "Page of patients returned")
    public Page<PatientResponse> listPatients(Pageable pageable) {
        return patientService.listPatients(pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR') or #id == authentication.principal.patientId")
    @Operation(summary = "Get a patient by id (a PATIENT may only fetch their own record)")
    @ApiResponse(responseCode = "200", description = "Patient found")
    @ApiResponse(responseCode = "403", description = "A PATIENT tried to read another patient's record")
    @ApiResponse(responseCode = "404", description = "Patient not found")
    public PatientResponse getPatient(@PathVariable Long id) {
        return patientService.getPatient(id);
    }

    // Pageable binds page/size/sort from query params automatically (Spring Boot auto-configures
    // this whenever Spring Data + Spring MVC are both present). A client-supplied sort is layered
    // on top of, not a replacement for, PatientRepository's scheduledAt-desc ordering - that
    // ordering is a business decision (most recent visit first), not something callers override.
    @GetMapping("/{id}/history")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR') or #id == authentication.principal.patientId")
    @Operation(summary = "Get a patient's paginated appointment history, most recent first")
    @ApiResponse(responseCode = "200", description = "History page returned")
    @ApiResponse(responseCode = "403", description = "A PATIENT tried to read another patient's history")
    @ApiResponse(responseCode = "404", description = "Patient not found")
    public Page<AppointmentResponse> getPatientHistory(@PathVariable Long id, Pageable pageable) {
        return patientService.getPatientHistory(id, pageable);
    }
}
