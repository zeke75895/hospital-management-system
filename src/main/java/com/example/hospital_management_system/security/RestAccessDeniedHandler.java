package com.example.hospital_management_system.security;

import com.example.hospital_management_system.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

// Handles authorizeHttpRequests URL-pattern denials (e.g. a PATIENT hitting POST /api/doctors) -
// also at the filter level, before DispatcherServlet. @PreAuthorize denials are a separate case:
// those happen INSIDE the controller invocation, so they reach GlobalExceptionHandler's
// AccessDeniedException handler instead. Both are needed for every 403 in this app to carry the
// same ErrorResponse shape regardless of which layer rejected the request.
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), new ErrorResponse("Access denied"));
    }
}
