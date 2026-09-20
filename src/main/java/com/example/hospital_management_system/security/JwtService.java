package com.example.hospital_management_system.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(
            @Value("${jwt.secret}") String secret, @Value("${jwt.expiration-ms:3600000}") long expirationMs) {
        // HS256 requires a key of at least 256 bits (32 bytes) once UTF-8 encoded, or jjwt throws
        // WeakKeyException at startup - the configured default secret is deliberately long enough.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    // patientId/doctorId are included purely for the client's convenience (e.g. the frontend
    // decoding its own token to know "who am I" without an extra round trip) - never for
    // authorization. A JWT payload is base64, not encrypted, so it was already readable by
    // whoever holds the token; adding these doesn't change what's trusted server-side. Every
    // @PreAuthorize/@AppointmentSecurity check still re-derives these from the database via
    // UserDetailsServiceImpl on every request, exactly as before.
    public String generateToken(AppUserDetails principal) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
                .subject(principal.getUsername())
                .claim("role", principal.getRole().name());
        if (principal.getPatientId() != null) {
            builder.claim("patientId", principal.getPatientId());
        }
        if (principal.getDoctorId() != null) {
            builder.claim("doctorId", principal.getDoctorId());
        }
        return builder.issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    // Only checks signature + expiry, deliberately not the "role" claim: authorization always
    // re-derives the caller's role and patientId/doctorId from the database via
    // UserDetailsServiceImpl on every request, never from what the token claims. If a user's role
    // changes after a token was issued, that change takes effect on their very next request
    // instead of waiting for the old token to expire.
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
