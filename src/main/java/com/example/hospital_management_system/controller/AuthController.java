package com.example.hospital_management_system.controller;

import com.example.hospital_management_system.dto.request.LoginRequest;
import com.example.hospital_management_system.dto.request.RegisterRequest;
import com.example.hospital_management_system.dto.response.AuthResponse;
import com.example.hospital_management_system.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Issue login credentials for an existing Patient or Doctor record (or an ADMIN)")
    @ApiResponse(responseCode = "201", description = "User created")
    @ApiResponse(responseCode = "400", description = "Validation failed")
    @ApiResponse(responseCode = "404", description = "Referenced patientId/doctorId not found")
    @ApiResponse(responseCode = "409", description = "Username already taken")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    @Operation(summary = "Exchange username/password for a JWT")
    @ApiResponse(responseCode = "200", description = "Login succeeded")
    @ApiResponse(responseCode = "401", description = "Invalid username or password")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
