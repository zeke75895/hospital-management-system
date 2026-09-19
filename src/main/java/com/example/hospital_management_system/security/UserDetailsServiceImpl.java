package com.example.hospital_management_system.security;

import com.example.hospital_management_system.entity.User;
import com.example.hospital_management_system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Defining this bean is also what suppresses Spring Boot's "generate a random console password"
// default user: that autoconfiguration only kicks in when no UserDetailsService bean is present.
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    // @Transactional matters here, not just for consistency: User.patient/User.doctor are LAZY,
    // and this is the one place their ids get resolved into AppUserDetails' plain fields - has to
    // happen while the session is still open.
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) {
        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("No user with username: " + username));

        Long patientId = user.getPatient() == null ? null : user.getPatient().getId();
        Long doctorId = user.getDoctor() == null ? null : user.getDoctor().getId();

        return new AppUserDetails(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getRole(),
                patientId,
                doctorId,
                user.isEnabled());
    }
}
