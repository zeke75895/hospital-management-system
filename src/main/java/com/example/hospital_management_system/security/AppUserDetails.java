package com.example.hospital_management_system.security;

import com.example.hospital_management_system.entity.Role;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

// Plain fields, not a reference to the User entity: UserDetailsServiceImpl builds this from
// inside a @Transactional method, where User.patient/User.doctor (both LAZY) can still be
// resolved. Holding onto the entity itself instead of extracting the ids up front would risk a
// LazyInitializationException the moment @PreAuthorize/@AppointmentSecurity reads
// getPatientId()/getDoctorId() later, well outside that transaction.
//
// No @Getter here deliberately: Lombok would generate isEnabled() for the `enabled` field, which
// collides with the UserDetails-mandated isEnabled() override below - not a style choice, an
// actual duplicate-method compile error, so the accessors below are written out by hand.
public class AppUserDetails implements UserDetails {

    private final Long userId;
    private final String username;
    private final String passwordHash;
    private final Role role;
    private final Long patientId;
    private final Long doctorId;
    private final boolean enabled;

    public AppUserDetails(
            Long userId,
            String username,
            String passwordHash,
            Role role,
            Long patientId,
            Long doctorId,
            boolean enabled) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.enabled = enabled;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    public Long getPatientId() {
        return patientId;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
