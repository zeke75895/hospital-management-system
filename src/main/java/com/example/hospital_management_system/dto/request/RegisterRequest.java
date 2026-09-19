package com.example.hospital_management_system.dto.request;

import com.example.hospital_management_system.entity.Role;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Registration links a NEW login to an EXISTING Patient/Doctor record rather than creating one:
// POST /api/patients and POST /api/doctors (staff-only) are still how a Patient/Doctor record
// itself comes into being - this only issues credentials for one that already exists. ADMIN needs
// neither id.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank
    @Size(max = 100)
    private String username;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    @NotNull
    private Role role;

    private Long patientId;

    private Long doctorId;

    @AssertTrue(message = "patientId is required when role is PATIENT, and must be omitted otherwise")
    public boolean isPatientLinkValid() {
        return role != Role.PATIENT || patientId != null;
    }

    @AssertTrue(message = "doctorId is required when role is DOCTOR, and must be omitted otherwise")
    public boolean isDoctorLinkValid() {
        return role != Role.DOCTOR || doctorId != null;
    }
}
