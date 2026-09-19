package com.example.hospital_management_system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.hospital_management_system.dto.request.AppointmentRequest;
import com.example.hospital_management_system.dto.response.AppointmentResponse;
import com.example.hospital_management_system.entity.Appointment;
import com.example.hospital_management_system.entity.AppointmentStatus;
import com.example.hospital_management_system.entity.Doctor;
import com.example.hospital_management_system.entity.Patient;
import com.example.hospital_management_system.exception.DoctorNotAvailableException;
import com.example.hospital_management_system.exception.DoubleBookingException;
import com.example.hospital_management_system.mapper.AppointmentMapper;
import com.example.hospital_management_system.repository.AppointmentRepository;
import com.example.hospital_management_system.repository.DoctorRepository;
import com.example.hospital_management_system.repository.PatientRepository;
import com.example.hospital_management_system.repository.ScheduleRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pure Mockito unit tests for AppointmentService.bookAppointment - no Spring context, no
 * database. Each test proves one branch of the method's logic in isolation: that the
 * happy path actually saves and returns a response, that a conflicting appointment blocks
 * the booking instead of silently succeeding, and that a time outside the doctor's schedule
 * is rejected before a conflict check is even attempted. None of these prove the pessimistic
 * locking actually prevents a race under real concurrency - that needs real threads and a
 * real database, which is what AppointmentConcurrencyTest is for.
 *
 * <p>AppointmentService is constructed manually rather than via @InjectMocks: Mockito's
 * constructor-injection strategy for @InjectMocks only fills parameters from @Mock/@Spy
 * fields, so the Clock parameter (deliberately not a Mockito mock - it's a plain value type)
 * would silently come through as null. Building the service by hand keeps that explicit.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    private static final Long PATIENT_ID = 10L;
    private static final Long DOCTOR_ID = 20L;
    private static final LocalDateTime SCHEDULED_AT = LocalDateTime.of(2030, 6, 10, 10, 0);

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private AppointmentMapper appointmentMapper;

    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentService(
                appointmentRepository,
                doctorRepository,
                patientRepository,
                scheduleRepository,
                appointmentMapper,
                Clock.systemDefaultZone());
    }

    private AppointmentRequest.AppointmentRequestBuilder validRequestBuilder() {
        return AppointmentRequest.builder()
                .patientId(PATIENT_ID)
                .doctorId(DOCTOR_ID)
                .scheduledAt(SCHEDULED_AT)
                .reason("Annual checkup");
    }

    @Test
    void bookAppointment_happyPath_savesAndReturnsResponse() {
        Patient patient = Patient.builder().id(PATIENT_ID).build();
        Doctor doctor = Doctor.builder().id(DOCTOR_ID).build();
        AppointmentRequest request = validRequestBuilder().build();
        Appointment mappedAppointment = Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .scheduledAt(SCHEDULED_AT)
                .status(AppointmentStatus.BOOKED)
                .reason("Annual checkup")
                .build();
        Appointment savedAppointment = Appointment.builder()
                .id(99L)
                .patient(patient)
                .doctor(doctor)
                .scheduledAt(SCHEDULED_AT)
                .status(AppointmentStatus.BOOKED)
                .reason("Annual checkup")
                .build();
        AppointmentResponse expectedResponse = AppointmentResponse.builder().id(99L).build();

        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(doctorRepository.findByIdForUpdate(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(scheduleRepository.existsSlotCoveringTime(DOCTOR_ID, SCHEDULED_AT)).thenReturn(true);
        when(appointmentRepository.findConflictingAppointmentForUpdate(
                        DOCTOR_ID, SCHEDULED_AT, AppointmentStatus.BOOKED))
                .thenReturn(List.of());
        when(appointmentMapper.toEntity(request, patient, doctor)).thenReturn(mappedAppointment);
        when(appointmentRepository.save(mappedAppointment)).thenReturn(savedAppointment);
        when(appointmentMapper.toResponse(savedAppointment)).thenReturn(expectedResponse);

        AppointmentResponse result = appointmentService.bookAppointment(request);

        assertThat(result).isEqualTo(expectedResponse);
        verify(appointmentRepository).save(mappedAppointment);
    }

    @Test
    void bookAppointment_conflictingAppointmentExists_throwsDoubleBookingExceptionAndDoesNotSave() {
        Patient patient = Patient.builder().id(PATIENT_ID).build();
        Doctor doctor = Doctor.builder().id(DOCTOR_ID).build();
        AppointmentRequest request = validRequestBuilder().build();
        Appointment existingConflict = Appointment.builder().id(5L).build();

        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(doctorRepository.findByIdForUpdate(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(scheduleRepository.existsSlotCoveringTime(DOCTOR_ID, SCHEDULED_AT)).thenReturn(true);
        when(appointmentRepository.findConflictingAppointmentForUpdate(
                        DOCTOR_ID, SCHEDULED_AT, AppointmentStatus.BOOKED))
                .thenReturn(List.of(existingConflict));

        assertThatThrownBy(() -> appointmentService.bookAppointment(request))
                .isInstanceOf(DoubleBookingException.class);

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void bookAppointment_outsideScheduleWindow_throwsDoctorNotAvailableExceptionBeforeCheckingConflicts() {
        Patient patient = Patient.builder().id(PATIENT_ID).build();
        Doctor doctor = Doctor.builder().id(DOCTOR_ID).build();
        AppointmentRequest request = validRequestBuilder().build();

        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(doctorRepository.findByIdForUpdate(DOCTOR_ID)).thenReturn(Optional.of(doctor));
        when(scheduleRepository.existsSlotCoveringTime(DOCTOR_ID, SCHEDULED_AT)).thenReturn(false);

        assertThatThrownBy(() -> appointmentService.bookAppointment(request))
                .isInstanceOf(DoctorNotAvailableException.class);

        // Proves the schedule-window check short-circuits before the (pessimistic-locked, more
        // expensive) conflict query ever runs - not just that the right exception surfaces.
        verify(appointmentRepository, never()).findConflictingAppointmentForUpdate(any(), any(), any());
        verify(appointmentRepository, never()).save(any());
    }
}
