package com.example.hospital_management_system.dto.response;

public record AuthResponse(String token, String tokenType, long expiresInSeconds) {}
