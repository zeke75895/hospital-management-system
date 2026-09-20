package com.example.hospital_management_system.config;

import com.example.hospital_management_system.security.JwtAuthenticationFilter;
import com.example.hospital_management_system.security.RestAccessDeniedHandler;
import com.example.hospital_management_system.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// CSRF stays disabled and sessions stay STATELESS (unchanged from before): this is a per-request
// JWT-bearer API with no ambient session/cookie credential for CSRF to protect.
//
// @EnableMethodSecurity turns on @PreAuthorize/@PostAuthorize processing - required for the
// per-resource ownership checks on PatientController and AppointmentController, which URL
// patterns alone can't express (see AppointmentSecurity's Javadoc for exactly why).
//
// Coarse, role-only rules live here as requestMatchers (checked once, before a controller is even
// invoked); rules that depend on the value of a specific id or request body live as @PreAuthorize
// on the controller method instead (checked with full access to method arguments and the
// authenticated principal). Both are used deliberately, not just one or the other - see each
// annotated controller method for which category it falls into and why.
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**")
                        .permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                        .permitAll()
                        // The static frontend (src/main/resources/static/) - it does its own login
                        // screen and calls /api/** with a JWT afterward, so the page shell itself
                        // has to be reachable before any authentication exists.
                        .requestMatchers("/", "/index.html", "/css/**", "/js/**")
                        .permitAll()
                        // Pure role checks, no per-resource identity involved - a URL pattern is
                        // sufficient and cheaper than a method-security check for these.
                        .requestMatchers(HttpMethod.POST, "/api/patients/**")
                        .hasAnyRole("DOCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/doctors/**")
                        .hasAnyRole("DOCTOR", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/appointments/*/complete")
                        .hasAnyRole("DOCTOR", "ADMIN")
                        // Everything else under /api/** just needs to be authenticated; ownership
                        // (a PATIENT only seeing their own patient/appointment records) is enforced
                        // by @PreAuthorize on the specific controller methods, not here.
                        .anyRequest()
                        .authenticated())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Explicit AuthenticationManager bean, built from AuthenticationConfiguration - the standard
    // way to obtain one for manual use (AuthService.login()'s authenticationManager.authenticate()
    // call) since Spring Security 6 removed WebSecurityConfigurerAdapter. Spring Boot wires this to
    // a DaoAuthenticationProvider using the single UserDetailsService and PasswordEncoder beans
    // present in the context, with no extra config needed.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
