package com.example.hospital_management_system.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Plain Bean Validation tests - no Spring context, no database. Validator.validate() runs every
 * constraint annotation on the object directly, exactly what @Valid triggers at the controller
 * boundary, just without needing a live HTTP request to exercise it.
 */
class RequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Nested
    class PatientRequestValidation {

        private PatientRequest.PatientRequestBuilder valid() {
            return PatientRequest.builder()
                    .firstName("Alice")
                    .lastName("Smith")
                    .dateOfBirth(LocalDate.of(1990, 1, 1))
                    .email("alice@example.com")
                    .phone("555-0100")
                    .medicalRecordNumber("MRN-1");
        }

        @Test
        void validRequest_hasNoViolations() {
            assertThat(validator.validate(valid().build())).isEmpty();
        }

        @Test
        void blankFirstName_isRejected() {
            Set<ConstraintViolation<PatientRequest>> violations =
                    validator.validate(valid().firstName("   ").build());

            assertThat(violations)
                    .extracting(v -> v.getPropertyPath().toString())
                    .contains("firstName");
        }

        @Test
        void invalidEmail_isRejected() {
            Set<ConstraintViolation<PatientRequest>> violations =
                    validator.validate(valid().email("not-an-email").build());

            assertThat(violations)
                    .extracting(v -> v.getPropertyPath().toString())
                    .contains("email");
        }

        @Test
        void futureDateOfBirth_isRejected() {
            Set<ConstraintViolation<PatientRequest>> violations =
                    validator.validate(valid().dateOfBirth(LocalDate.now().plusDays(1)).build());

            assertThat(violations)
                    .extracting(v -> v.getPropertyPath().toString())
                    .contains("dateOfBirth");
        }
    }

    @Nested
    class AppointmentRequestValidation {

        // validateProperty(), not validate(): validating the whole object would also run the
        // class-level @ValidAppointmentSlot constraint. Its AppointmentSlotValidator needs Spring
        // to inject a ScheduleRepository, which this plain, no-Spring-context Validator can't
        // provide - it would throw trying to instantiate it. validateProperty() runs only the
        // constraints declared on the one named field, sidestepping the class-level constraint
        // entirely. @ValidAppointmentSlot is already covered separately - by
        // AppointmentServiceTest's "outside schedule window" case, and by the curl verification
        // during the controllers turn.
        private AppointmentRequest.AppointmentRequestBuilder valid() {
            return AppointmentRequest.builder()
                    .patientId(1L)
                    .doctorId(1L)
                    .scheduledAt(LocalDateTime.now().plusDays(1))
                    .reason("Checkup");
        }

        @Test
        void pastScheduledAt_isRejected() {
            AppointmentRequest request = valid().scheduledAt(LocalDateTime.now().minusDays(1)).build();

            Set<ConstraintViolation<AppointmentRequest>> violations =
                    validator.validateProperty(request, "scheduledAt");

            assertThat(violations).isNotEmpty();
        }

        @Test
        void blankReason_isRejected() {
            AppointmentRequest request = valid().reason("   ").build();

            Set<ConstraintViolation<AppointmentRequest>> violations = validator.validateProperty(request, "reason");

            assertThat(violations).isNotEmpty();
        }

        @Test
        void nullPatientId_isRejected() {
            AppointmentRequest request = valid().patientId(null).build();

            Set<ConstraintViolation<AppointmentRequest>> violations =
                    validator.validateProperty(request, "patientId");

            assertThat(violations).isNotEmpty();
        }
    }
}
