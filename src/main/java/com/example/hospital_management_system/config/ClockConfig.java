package com.example.hospital_management_system.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Services inject Clock instead of calling LocalDateTime.now() directly, so tests (e.g.
// AppointmentService's "cancel an already-past appointment" case) can supply a fixed Clock
// instead of depending on the real wall-clock time.
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
