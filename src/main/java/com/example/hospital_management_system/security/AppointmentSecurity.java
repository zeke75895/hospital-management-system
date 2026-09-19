package com.example.hospital_management_system.security;

import com.example.hospital_management_system.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Referenced from @PreAuthorize as @appointmentSecurity.isOwnedByCurrentPatient(...) - a URL ant
// pattern can express "this whole path needs authentication" or "this whole path needs role X",
// but it has no way to express "the :id in this specific path must belong to the caller," since
// that requires comparing the PATH VALUE against data loaded from the database, not just matching
// a string pattern. This bean is what @PreAuthorize's SpEL calls out to for exactly that check.
//
// A nonexistent appointmentId also returns false here (access denied) rather than distinguishing
// "not yours" from "doesn't exist" - a deliberate simplification. It means a bad id surfaces as
// 403 instead of 404 when denied at this layer, trading slightly confusing semantics for not
// revealing to an unauthorized caller whether a given id exists at all.
@Component("appointmentSecurity")
@RequiredArgsConstructor
public class AppointmentSecurity {

    private final AppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public boolean isOwnedByCurrentPatient(Long appointmentId, Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof AppUserDetails principal) || principal.getPatientId() == null) {
            return false;
        }

        return appointmentRepository
                .findById(appointmentId)
                .map(appointment -> appointment.getPatient().getId().equals(principal.getPatientId()))
                .orElse(false);
    }
}
