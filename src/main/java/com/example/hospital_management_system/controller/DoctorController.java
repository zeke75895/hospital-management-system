package com.example.hospital_management_system.controller;

import com.example.hospital_management_system.dto.request.DoctorRequest;
import com.example.hospital_management_system.dto.response.AvailabilityResponse;
import com.example.hospital_management_system.dto.response.DoctorResponse;
import com.example.hospital_management_system.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Tag(name = "Doctors")
@SecurityRequirement(name = "bearerAuth")
public class DoctorController {

    private final DoctorService doctorService;

    @PostMapping
    @Operation(summary = "Register a new doctor")
    @ApiResponse(responseCode = "201", description = "Doctor created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    public ResponseEntity<DoctorResponse> createDoctor(@Valid @RequestBody DoctorRequest request) {
        DoctorResponse response = doctorService.createDoctor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // No @PreAuthorize needed: doctor records aren't patient-owned data, so there's no ownership
    // case here, just "must be logged in" - already covered by SecurityConfig's blanket
    // .anyRequest().authenticated() rule for every role including PATIENT (they need to browse
    // doctors to pick one when booking).
    @GetMapping
    @Operation(summary = "List doctors")
    @ApiResponse(responseCode = "200", description = "Page of doctors returned")
    public Page<DoctorResponse> listDoctors(Pageable pageable) {
        return doctorService.listDoctors(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a doctor by id")
    @ApiResponse(responseCode = "200", description = "Doctor found")
    @ApiResponse(responseCode = "404", description = "Doctor not found")
    public DoctorResponse getDoctor(@PathVariable Long id) {
        return doctorService.getDoctor(id);
    }

    // Advisory only - see DoctorService.isDoctorAvailable's Javadoc. This tells a caller "you
    // could probably book here", never "you may book here without checking again"; the actual
    // guard lives in AppointmentService.bookAppointment's pessimistic locking.
    @GetMapping("/{id}/availability")
    @Operation(
            summary = "Check whether a doctor is free for a time window",
            description = "Advisory only: a non-locking read, not a booking guarantee. "
                    + "Re-checked authoritatively at booking time.")
    @ApiResponse(responseCode = "200", description = "Availability computed")
    @ApiResponse(responseCode = "404", description = "Doctor not found")
    public AvailabilityResponse checkAvailability(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return new AvailabilityResponse(doctorService.isDoctorAvailable(id, from, to));
    }
}
