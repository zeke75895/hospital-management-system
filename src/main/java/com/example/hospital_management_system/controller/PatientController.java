package com.example.hospital_management_system.controller;

import com.example.hospital_management_system.dto.request.PatientRequest;
import com.example.hospital_management_system.dto.response.AppointmentResponse;
import com.example.hospital_management_system.dto.response.PatientResponse;
import com.example.hospital_management_system.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/{id}")
    @Operation(summary = "Get a patient by id")
    @ApiResponse(responseCode = "200", description = "Patient found")
    @ApiResponse(responseCode = "404", description = "Patient not found")
    public PatientResponse getPatient(@PathVariable Long id) {
        return patientService.getPatient(id);
    }

    // Pageable binds page/size/sort from query params automatically (Spring Boot auto-configures
    // this whenever Spring Data + Spring MVC are both present). A client-supplied sort is layered
    // on top of, not a replacement for, PatientRepository's scheduledAt-desc ordering - that
    // ordering is a business decision (most recent visit first), not something callers override.
    @GetMapping("/{id}/history")
    @Operation(summary = "Get a patient's paginated appointment history, most recent first")
    @ApiResponse(responseCode = "200", description = "History page returned")
    @ApiResponse(responseCode = "404", description = "Patient not found")
    public Page<AppointmentResponse> getPatientHistory(@PathVariable Long id, Pageable pageable) {
        return patientService.getPatientHistory(id, pageable);
    }
}
