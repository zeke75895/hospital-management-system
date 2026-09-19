package com.example.hospital_management_system.controller;

import com.example.hospital_management_system.dto.request.AppointmentRequest;
import com.example.hospital_management_system.dto.response.AppointmentResponse;
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
    @PostMapping
    @Operation(summary = "Book a new appointment")
    @ApiResponse(responseCode = "201", description = "Appointment booked")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Patient or doctor not found")
    @ApiResponse(responseCode = "409", description = "Doctor unavailable, or already booked at this time")
    public ResponseEntity<AppointmentResponse> bookAppointment(@Valid @RequestBody AppointmentRequest request) {
        AppointmentResponse response = appointmentService.bookAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel a booked appointment")
    @ApiResponse(responseCode = "204", description = "Appointment cancelled")
    @ApiResponse(responseCode = "404", description = "Appointment not found")
    @ApiResponse(responseCode = "409", description = "Appointment isn't BOOKED, or has already passed")
    public ResponseEntity<Void> cancelAppointment(@PathVariable Long id) {
        appointmentService.cancelAppointment(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Mark a booked appointment as completed")
    @ApiResponse(responseCode = "200", description = "Appointment marked completed")
    @ApiResponse(responseCode = "404", description = "Appointment not found")
    @ApiResponse(responseCode = "409", description = "Appointment isn't BOOKED")
    public AppointmentResponse completeAppointment(@PathVariable Long id) {
        return appointmentService.completeAppointment(id);
    }

    @GetMapping
    @Operation(summary = "Search appointments by doctor, patient, and/or date (all filters optional)")
    @ApiResponse(responseCode = "200", description = "Matching appointments returned")
    public List<AppointmentResponse> searchAppointments(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return appointmentService.searchAppointments(doctorId, patientId, date);
    }
}
