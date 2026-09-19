package com.example.hospital_management_system.controller;

import com.example.hospital_management_system.dto.request.ScheduleRequest;
import com.example.hospital_management_system.dto.response.ScheduleResponse;
import com.example.hospital_management_system.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/doctors/{doctorId}/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping
    @Operation(summary = "Add a schedule window for a doctor")
    @ApiResponse(responseCode = "201", description = "Schedule created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Doctor not found")
    public ResponseEntity<ScheduleResponse> createSchedule(
            @PathVariable Long doctorId, @Valid @RequestBody ScheduleRequest request) {
        ScheduleResponse response = scheduleService.createSchedule(doctorId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
