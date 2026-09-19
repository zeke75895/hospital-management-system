package com.example.hospital_management_system.dto.response;

import com.example.hospital_management_system.entity.AppointmentStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentResponse {
    private Long id;
    private Long patientId;
    private String patientName;
    private Long doctorId;
    private String doctorName;
    private LocalDateTime scheduledAt;
    private AppointmentStatus status;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
