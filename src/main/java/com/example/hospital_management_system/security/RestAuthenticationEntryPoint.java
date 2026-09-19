package com.example.hospital_management_system.security;

import com.example.hospital_management_system.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

// Handles the "no/invalid token on a protected path" case, which happens at the security-filter
// level - BEFORE DispatcherServlet is ever invoked. GlobalExceptionHandler's @ExceptionHandler
// methods only see exceptions thrown from inside a controller/service call, so they can't reach
// this case; this is the filter-chain equivalent, wired in via SecurityConfig's
// exceptionHandling(). Without it, this path would fall through to Spring Security's bare default
// response instead of our ErrorResponse shape.
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getWriter(), new ErrorResponse("Authentication required: missing or invalid token"));
    }
}
