package com.example.hospital_management_system.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.hospital_management_system.dto.request.AppointmentRequest;
import com.example.hospital_management_system.entity.Doctor;
import com.example.hospital_management_system.entity.Patient;
import com.example.hospital_management_system.entity.Schedule;
import com.example.hospital_management_system.exception.DoubleBookingException;
import com.example.hospital_management_system.repository.AppointmentRepository;
import com.example.hospital_management_system.repository.DoctorRepository;
import com.example.hospital_management_system.repository.PatientRepository;
import com.example.hospital_management_system.repository.ScheduleRepository;
import com.example.hospital_management_system.service.AppointmentService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Proves the pessimistic-locking design actually prevents a double-booking race under real
 * concurrent load, against a real database - not just that the code looks correct.
 *
 * <p>Deliberately NOT @Transactional (unlike AppointmentBookingIntegrationTest): a test-managed
 * transaction wraps the whole test method in one connection/transaction that only ever rolls
 * back, never commits. The doctor/patients/schedule created in setup would stay invisible to
 * every worker thread below, since each opens its own independent transaction (via
 * AppointmentService.bookAppointment's own @Transactional) on its own connection, and a separate
 * transaction can't see another transaction's uncommitted writes. Plain repository.save() calls,
 * by contrast, commit immediately on their own (Spring Data's CRUD methods are themselves
 * transactional), so the setup below really is visible to every thread once it returns.
 *
 * <p>This means the rows this test creates are NOT cleaned up afterward - each uses a unique
 * email/license/MRN suffix to avoid colliding with other tests sharing the same H2 instance
 * within one test run.
 */
@SpringBootTest
@ActiveProfiles("dev")
class AppointmentConcurrencyTest {

    private static final int THREAD_COUNT = 8;

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
    void concurrentBookingsForSameSlot_exactlyOneSucceeds() throws InterruptedException {
        Doctor doctor = doctorRepository.save(Doctor.builder()
                .firstName("Concurrency")
                .lastName("Test")
                .specialty("Diagnostics")
                .email("concurrency.doctor@example.com")
                .licenseNumber("LIC-CONC-001")
                .build());

        LocalDate scheduledDate = LocalDate.now().plusDays(21);
        LocalDateTime scheduledAt = scheduledDate.atTime(10, 0);

        scheduleRepository.save(Schedule.builder()
                .doctor(doctor)
                .dayOfWeek(scheduledDate.getDayOfWeek())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(17, 0))
                .build());

        // One patient per thread, all racing for the SAME doctor+scheduledAt slot - a shared
        // patientId isn't needed (and would be wrong: booking twice for the same patient at the
        // same time isn't the scenario being tested here; two different patients racing for one
        // doctor's one open slot is).
        List<Patient> patients = IntStream.range(0, THREAD_COUNT)
                .mapToObj(i -> patientRepository.save(Patient.builder()
                        .firstName("Patient")
                        .lastName("Number" + i)
                        .dateOfBirth(LocalDate.of(1990, 1, 1))
                        .email("concurrency.patient" + i + "@example.com")
                        .phone("555-020" + i)
                        .medicalRecordNumber("MRN-CONC-" + i)
                        .build()))
                .toList();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        // readyLatch/startLatch releases every thread at (as close to) the same instant, instead
        // of them trickling in one at a time - without this, the pessimistic lock could trivially
        // "work" just because requests never actually overlapped.
        CountDownLatch readyLatch = new CountDownLatch(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();

        for (Patient patient : patients) {
            Callable<String> bookingAttempt = () -> {
                readyLatch.countDown();
                startLatch.await();
                try {
                    appointmentService.bookAppointment(AppointmentRequest.builder()
                            .patientId(patient.getId())
                            .doctorId(doctor.getId())
                            .scheduledAt(scheduledAt)
                            .reason("Concurrency test")
                            .build());
                    return "SUCCESS";
                } catch (DoubleBookingException e) {
                    return "DOUBLE_BOOKING";
                }
            };
            futures.add(executor.submit(bookingAttempt));
        }

        readyLatch.await();
        startLatch.countDown();
        executor.shutdown();
        boolean finishedInTime = executor.awaitTermination(30, TimeUnit.SECONDS);
        assertThat(finishedInTime).as("all booking attempts finished within the timeout").isTrue();

        List<String> outcomes = new ArrayList<>();
        for (Future<String> future : futures) {
            try {
                outcomes.add(future.get());
            } catch (ExecutionException e) {
                // Any exception OTHER than DoubleBookingException (which is caught above and
                // returned as a normal value, not rethrown) surfaces here - failing the test
                // loudly instead of being silently miscounted as neither a success nor an
                // expected conflict.
                throw new AssertionError("Unexpected exception from a booking attempt", e.getCause());
            }
        }

        long successCount = outcomes.stream().filter("SUCCESS"::equals).count();
        long conflictCount = outcomes.stream().filter("DOUBLE_BOOKING"::equals).count();

        assertThat(successCount).as("exactly one booking should win the race").isEqualTo(1);
        assertThat(conflictCount)
                .as("every other attempt should be rejected as a double booking")
                .isEqualTo(THREAD_COUNT - 1);

        // Confirms the outcome at the database level too, not just via the in-memory outcome
        // list: exactly one row for this doctor at this exact timestamp, no duplicates that a
        // broken lock would have let through.
        List<?> persistedAtThisSlot = appointmentRepository.findByDoctorIdAndScheduledAtBetween(
                doctor.getId(), scheduledAt, scheduledAt.plusSeconds(1));
        assertThat(persistedAtThisSlot).hasSize(1);
    }
}
