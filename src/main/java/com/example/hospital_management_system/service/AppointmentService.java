package com.example.hospital_management_system.service;

import com.example.hospital_management_system.dto.request.AppointmentRequest;
import com.example.hospital_management_system.dto.response.AppointmentResponse;
import com.example.hospital_management_system.entity.Appointment;
import com.example.hospital_management_system.entity.AppointmentStatus;
import com.example.hospital_management_system.entity.Doctor;
import com.example.hospital_management_system.entity.Patient;
import com.example.hospital_management_system.exception.DoctorNotAvailableException;
import com.example.hospital_management_system.exception.DoubleBookingException;
import com.example.hospital_management_system.exception.InvalidAppointmentStateException;
import com.example.hospital_management_system.exception.ResourceNotFoundException;
import com.example.hospital_management_system.mapper.AppointmentMapper;
import com.example.hospital_management_system.repository.AppointmentRepository;
import com.example.hospital_management_system.repository.DoctorRepository;
import com.example.hospital_management_system.repository.PatientRepository;
import com.example.hospital_management_system.repository.ScheduleRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final ScheduleRepository scheduleRepository;
    private final AppointmentMapper appointmentMapper;
    private final Clock clock;

    /**
     * Propagation: left at the default, Propagation.REQUIRED - deliberately not overridden.
     * REQUIRED means "join the caller's transaction if one is already open, otherwise start a new
     * one." That's correct here because there's no legitimate reason a single booking should be
     * independently committable from whatever operation invoked it: if this is called directly
     * (e.g. from a future controller), REQUIRED just starts a fresh transaction, indistinguishable
     * from REQUIRES_NEW. But if a future caller wraps several bookings in its own @Transactional
     * method (e.g. a batch-booking endpoint that should commit or roll back as one unit),
     * REQUIRED lets this method participate in that outer transaction, while REQUIRES_NEW would
     * force a separate one and silently break that atomicity - one booking could commit while a
     * sibling in the same batch rolls back. Nothing about booking a single appointment needs its
     * own isolated transaction, so there's no reason to reach for anything other than the default.
     *
     * Everything below - the doctor-row lock, the schedule check, the conflict check, and the
     * final insert - runs inside the ONE transaction this annotation opens. That matters because a
     * pessimistic lock is only held for as long as its transaction stays open: it's taken by
     * findByIdForUpdate() below and released automatically the instant this method returns
     * (commit) or throws (rollback). If any of this ran across multiple transactions, the lock
     * would already be gone by the time the conflict check ran, and the whole guard would be
     * pointless.
     */
    @Transactional
    public AppointmentResponse bookAppointment(AppointmentRequest request) {
        Patient patient = patientRepository
                .findById(request.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + request.getPatientId()));

        // Locks the Doctor row for the rest of this transaction. This has to happen BEFORE the
        // conflict check below, not after - see DoctorRepository.findByIdForUpdate and
        // AppointmentRepository.findConflictingAppointmentForUpdate for why a lock on the
        // (possibly nonexistent) Appointment row alone can't prevent two concurrent requests from
        // both booking the same open slot.
        Doctor doctor = doctorRepository
                .findByIdForUpdate(request.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + request.getDoctorId()));

        // Step 2: is this doctor even scheduled to work at this time at all? Independent of
        // whether any conflicting appointment exists.
        if (!scheduleRepository.existsSlotCoveringTime(doctor.getId(), request.getScheduledAt())) {
            throw new DoctorNotAvailableException(
                    "Doctor " + doctor.getId() + " has no schedule covering " + request.getScheduledAt());
        }

        // Step 3: now that the doctor row is locked, check for a conflicting BOOKED appointment.
        // Race-free specifically because the lock above serializes every concurrent booking
        // attempt for this doctor down to one at a time - see the repository method's own comment
        // for the phantom-read gap this closes.
        List<Appointment> conflicts = appointmentRepository.findConflictingAppointmentForUpdate(
                doctor.getId(), request.getScheduledAt(), AppointmentStatus.BOOKED);

        if (!conflicts.isEmpty()) {
            // Step 4: DoubleBookingException is unchecked, so Spring's default rollback rule
            // applies with no extra config: this transaction rolls back, the doctor-row lock is
            // released immediately, and nothing is persisted. See the class-level Javadoc on why
            // this must be thrown, never caught-and-swallowed, inside this method.
            throw new DoubleBookingException(
                    "Doctor " + doctor.getId() + " already has a booked appointment at "
                            + request.getScheduledAt());
        }

        // Step 5: no conflict found while holding the lock - safe to insert.
        Appointment appointment = appointmentMapper.toEntity(request, patient, doctor);
        Appointment saved = appointmentRepository.save(appointment);
        return appointmentMapper.toResponse(saved);
    }

    /**
     * Only a BOOKED appointment that hasn't happened yet can be cancelled. Both checks are
     * enforced here, not just at the DB level, because "cancelled" is a business state
     * transition, not a data constraint the schema alone can express.
     */
    @Transactional
    public AppointmentResponse cancelAppointment(Long appointmentId) {
        Appointment appointment = findAppointmentOrThrow(appointmentId);

        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new InvalidAppointmentStateException(
                    "Only a BOOKED appointment can be cancelled; this one is " + appointment.getStatus());
        }
        if (!appointment.getScheduledAt().isAfter(LocalDateTime.now(clock))) {
            throw new InvalidAppointmentStateException("Cannot cancel an appointment that has already passed");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        // No explicit save(): `appointment` is managed, so dirty checking flushes this UPDATE at
        // commit, incrementing @Version as part of it. If another transaction already changed
        // this exact row (e.g. concurrently completing it) and committed first, that UPDATE's
        // WHERE id = ? AND version = ? matches zero rows, and Hibernate raises
        // ObjectOptimisticLockingFailureException here - unchecked, so it rolls this transaction
        // back too, and GlobalExceptionHandler maps it to 409.
        return appointmentMapper.toResponse(appointment);
    }

    /** Mirrors cancelAppointment's state check; completing an already-cancelled appointment (or
     * completing one twice) doesn't make sense as a state transition. */
    @Transactional
    public AppointmentResponse completeAppointment(Long appointmentId) {
        Appointment appointment = findAppointmentOrThrow(appointmentId);

        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new InvalidAppointmentStateException(
                    "Only a BOOKED appointment can be completed; this one is " + appointment.getStatus());
        }

        appointment.setStatus(AppointmentStatus.COMPLETED);
        return appointmentMapper.toResponse(appointment);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointment(Long id) {
        return appointmentMapper.toResponse(findAppointmentOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> listAppointmentsForDoctor(
            Long doctorId, LocalDateTime start, LocalDateTime end) {
        return appointmentRepository.findByDoctorIdAndScheduledAtBetween(doctorId, start, end).stream()
                .map(appointmentMapper::toResponse)
                .toList();
    }

    private Appointment findAppointmentOrThrow(Long id) {
        return appointmentRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));
    }
}
