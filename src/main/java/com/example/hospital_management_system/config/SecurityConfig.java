package com.example.hospital_management_system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

// Real auth (JWT) isn't built yet - that's still upcoming work. Until then, Spring Security's
// default behavior (every request needs the auto-generated console password) stays in place for
// /api/**: it's safer to leave endpoints locked down while this is built out incrementally than
// to accidentally open them early. This config carves out only the paths Swagger UI itself needs
// to load and render its docs page - it does not otherwise change /api/** authentication.
//
// httpBasic() has to be added back explicitly here: defining ANY SecurityFilterChain bean makes
// Spring Boot's autoconfigured default chain back off entirely, including the httpBasic +
// generated-password login it normally wires up for you. Without this line, /api/** would return
// a bare 403 with no way to authenticate at all - worse than doing nothing.
//
// CSRF is disabled and sessions are STATELESS: CSRF protection defends session/cookie-based
// browser auth against a malicious site forging requests using the victim's ambient session
// credentials. This is a JSON REST API authenticated per-request (Basic Auth now, JWT bearer
// tokens later) with no ambient session to forge, so CSRF protection has nothing to protect and
// only blocks legitimate POST/PUT/PATCH/DELETE calls - which is exactly what was happening before
// this fix (every write endpoint returned 401, not because auth failed, but because the CSRF
// filter rejected the request before the write ever reached a controller).
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth.requestMatchers(
                        "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }
}
