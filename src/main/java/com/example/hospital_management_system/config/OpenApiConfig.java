package com.example.hospital_management_system.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

// Registers the "bearerAuth" scheme referenced by @SecurityRequirement on
// Patient/Doctor/Schedule/AppointmentController. Springdoc has no way to infer this from
// JwtAuthenticationFilter on its own - without it, Swagger UI has no Authorize button at all, and
// every "Try it out" call would need its Authorization header pasted in by hand.
@Configuration
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {}
