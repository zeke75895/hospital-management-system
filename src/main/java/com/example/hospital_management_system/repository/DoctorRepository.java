package com.example.hospital_management_system.repository;

import com.example.hospital_management_system.entity.Doctor;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    /**
     * Locks the Doctor row for the rest of the enclosing transaction. This is the real
     * concurrency guard for booking, not AppointmentRepository's locked conflict query below —
     * see the comment there for why. AppointmentService.bookAppointment() must call this BEFORE
     * checking for a conflicting appointment, and hold it until the new Appointment is inserted.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Doctor d WHERE d.id = :id")
    Optional<Doctor> findByIdForUpdate(@Param("id") Long id);
}
