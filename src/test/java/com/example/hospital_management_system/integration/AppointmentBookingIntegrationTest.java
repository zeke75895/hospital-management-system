package com.example.hospital_management_system.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.hospital_management_system.dto.request.AppointmentRequest;
import com.example.hospital_management_system.dto.response.AppointmentResponse;
import com.example.hospital_management_system.entity.Appointment;
import com.example.hospital_management_system.entity.AppointmentStatus;
import com.example.hospital_management_system.entity.Doctor;
import com.example.hospital_management_system.entity.Patient;
import com.example.hospital_management_system.entity.Schedule;
import com.example.hospital_management_system.repository.AppointmentRepository;
import com.example.hospital_management_system.repository.DoctorRepository;
import com.example.hospital_management_system.repository.PatientRepository;
import com.example.hospital_management_system.repository.ScheduleRepository;
import com.example.hospital_management_system.service.AppointmentService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Boots the full Spring context against real H2 (the dev profile) and exercises
 * AppointmentService.bookAppointment through the actual repository/entity/Hibernate stack -
 * proving the wiring works end-to-end, not just that the service logic is correct in isolation
 * (AppointmentServiceTest already covers that with mocks).
 *
 * <p>@Transactional here is test-managed: Spring wraps the whole test method in one transaction
 * and rolls it back afterward, so the doctor/patient/schedule/appointment this test creates never
 * actually persists past the test method - no manual cleanup needed. This is safe here
 * specifically because everything runs on one thread; it would NOT be safe for the concurrency
 * test (see AppointmentConcurrencyTest's Javadoc for why).
 */
@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class AppointmentBookingIntegrationTest {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Test
    void bookAppointment_persistsAppointmentVisibleThroughTheRepository() {
        Doctor doctor = doctorRepository.save(Doctor.builder()
                .firstName("Greg")
                .lastName("House")
                .specialty("Diagnostics")
                .email("house.integration@example.com")
                .licenseNumber("LIC-INT-001")
                .build());

        Patient patient = patientRepository.save(Patient.builder()
                .firstName("Alice")
                .lastName("Smith")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .email("alice.integration@example.com")
                .phone("555-0100")
                .medicalRecordNumber("MRN-INT-001")
                .build());

        // Computed from "today" rather than hardcoded, so the schedule's dayOfWeek always matches
        // scheduledAt's actual day of week regardless of which day this test happens to run on.
        LocalDate scheduledDate = LocalDate.now().plusDays(14);
        LocalDateTime scheduledAt = scheduledDate.atTime(10, 0);

        scheduleRepository.save(Schedule.builder()
                .doctor(doctor)
                .dayOfWeek(scheduledDate.getDayOfWeek())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .build());

        AppointmentRequest request = AppointmentRequest.builder()
                .patientId(patient.getId())
                .doctorId(doctor.getId())
                .scheduledAt(scheduledAt)
                .reason("Integration test checkup")
                .build();

        AppointmentResponse response = appointmentService.bookAppointment(request);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo(AppointmentStatus.BOOKED);

        // Re-fetches independently from the repository rather than trusting the mapped response,
        // to prove the row actually landed in the database with the right shape - not just that
        // bookAppointment() returned something that looked right.
        Appointment persisted = appointmentRepository.findById(response.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(AppointmentStatus.BOOKED);
        assertThat(persisted.getPatient().getId()).isEqualTo(patient.getId());
        assertThat(persisted.getDoctor().getId()).isEqualTo(doctor.getId());
        assertThat(persisted.getScheduledAt()).isEqualTo(scheduledAt);
        assertThat(persisted.getReason()).isEqualTo("Integration test checkup");
        assertThat(persisted.getCreatedAt()).isNotNull();
        assertThat(persisted.getVersion()).isNotNull();
    }
}
