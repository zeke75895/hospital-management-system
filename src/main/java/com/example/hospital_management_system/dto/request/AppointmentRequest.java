package com.example.hospital_management_system.dto.request;

import com.example.hospital_management_system.validation.ValidAppointmentSlot;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// No status field here on purpose: status is server-assigned (BOOKED) at creation time.
// Cancel/complete are separate explicit operations later, not a generic field a client can set.
@ValidAppointmentSlot
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppointmentRequest {

    @NotNull
    private Long patientId;

    @NotNull
    private Long doctorId;

    @NotNull
    @Future
    private LocalDateTime scheduledAt;

    @NotBlank
    @Size(max = 500)
    private String reason;
}
