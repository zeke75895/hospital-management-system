package com.example.hospital_management_system.service;

import com.example.hospital_management_system.dto.request.LoginRequest;
import com.example.hospital_management_system.dto.request.RegisterRequest;
import com.example.hospital_management_system.dto.response.AuthResponse;
import com.example.hospital_management_system.entity.Doctor;
import com.example.hospital_management_system.entity.Patient;
import com.example.hospital_management_system.entity.Role;
import com.example.hospital_management_system.entity.User;
import com.example.hospital_management_system.exception.DuplicateUsernameException;
import com.example.hospital_management_system.exception.ResourceNotFoundException;
import com.example.hospital_management_system.repository.DoctorRepository;
import com.example.hospital_management_system.repository.PatientRepository;
import com.example.hospital_management_system.repository.UserRepository;
import com.example.hospital_management_system.security.AppUserDetails;
import com.example.hospital_management_system.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new DuplicateUsernameException("Username already taken: " + request.getUsername());
        }

        User.UserBuilder builder = User.builder()
                .username(request.getUsername())
                // BCrypt: salted, adaptive-cost hashing - never store or compare raw passwords.
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .enabled(true);

        if (request.getRole() == Role.PATIENT) {
            Patient patient = patientRepository
                    .findById(request.getPatientId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Patient not found: " + request.getPatientId()));
            builder.patient(patient);
        } else if (request.getRole() == Role.DOCTOR) {
            Doctor doctor = doctorRepository
                    .findById(request.getDoctorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + request.getDoctorId()));
            builder.doctor(doctor);
        }

        userRepository.save(builder.build());
    }

    // Delegates credential checking to AuthenticationManager rather than comparing passwords
    // directly here - it's what wires together UserDetailsServiceImpl (loads the user) and the
    // BCryptPasswordEncoder bean (verifies the hash) via the DaoAuthenticationProvider Spring Boot
    // autoconfigures when exactly one UserDetailsService and one PasswordEncoder bean are present.
    // Throws BadCredentialsException (unchecked) on a wrong username or password - GlobalExceptionHandler
    // maps that to 401. Deliberately the same exception/response for "no such user" and "wrong
    // password": telling an attacker which one failed would leak which usernames exist.
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);
        return new AuthResponse(token, "Bearer", jwtService.getExpirationSeconds());
    }
}
