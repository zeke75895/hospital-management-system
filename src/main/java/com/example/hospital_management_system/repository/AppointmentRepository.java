package com.example.hospital_management_system.repository;

import com.example.hospital_management_system.entity.Appointment;
import com.example.hospital_management_system.entity.AppointmentStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByDoctorIdAndScheduledAtBetween(
            Long doctorId, LocalDateTime start, LocalDateTime end);

    /** Non-locking existence check — fine for an "is this slot free?" display query, but not a
     * substitute for findConflictingAppointmentForUpdate() inside an actual booking transaction;
     * see that method's comment for why. */
    boolean existsByDoctorIdAndScheduledAtAndStatus(
            Long doctorId, LocalDateTime scheduledAt, AppointmentStatus status);

    Page<Appointment> findByPatientIdOrderByScheduledAtDesc(Long patientId, Pageable pageable);

    /**
     * The pessimistic conflict check used inside bookAppointment()'s transaction, called AFTER
     * DoctorRepository.findByIdForUpdate(doctorId) has already locked the doctor row.
     *
     * <p>Why the doctor lock has to come first: {@code SELECT ... FOR UPDATE} only locks rows
     * that already match the WHERE clause. When booking a fresh slot — the common case — this
     * query matches zero rows, and locking zero rows locks nothing. Two transactions racing to
     * book the same open slot would each run this query, each see no conflict, and both insert.
     * That's a classic phantom-read gap; row-level locking can't close it by itself.
     *
     * <p>Locking the Doctor row first (a row that's guaranteed to already exist) serializes every
     * booking attempt for that doctor, which closes the gap. This query then re-checks for a
     * conflict inside that lock — and if an existing BOOKED appointment does match, this locks it
     * too, so a concurrent cancel/complete on that exact appointment (handled separately via
     * {@code @Version} — see Appointment) can't interleave mid-booking.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId "
                    + "AND a.scheduledAt = :scheduledAt AND a.status = :status")
    List<Appointment> findConflictingAppointmentForUpdate(
            @Param("doctorId") Long doctorId,
            @Param("scheduledAt") LocalDateTime scheduledAt,
            @Param("status") AppointmentStatus status);
}
