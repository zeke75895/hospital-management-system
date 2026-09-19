package com.example.hospital_management_system.validation;

import com.example.hospital_management_system.dto.request.AppointmentRequest;
import com.example.hospital_management_system.entity.Schedule;
import com.example.hospital_management_system.repository.ScheduleRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// @Component matters here, not just for style: Spring Boot's ValidationAutoConfiguration wires
// LocalValidatorFactoryBean with a SpringConstraintValidatorFactory, which looks up an existing
// Spring bean of this type (with ScheduleRepository already injected) instead of instantiating
// this class with `new` and leaving the field null.
@Component
@RequiredArgsConstructor
public class AppointmentSlotValidator
        implements ConstraintValidator<ValidAppointmentSlot, AppointmentRequest> {

    private final ScheduleRepository scheduleRepository;

    @Override
    public boolean isValid(AppointmentRequest request, ConstraintValidatorContext context) {
        if (request == null || request.getDoctorId() == null || request.getScheduledAt() == null) {
            // Let @NotNull on the individual fields report those failures instead of a
            // confusing "invalid slot" message when the real problem is a missing field.
            return true;
        }

        LocalDateTime scheduledAt = request.getScheduledAt();
        LocalTime time = scheduledAt.toLocalTime();

        List<Schedule> schedules = scheduleRepository.findSchedulesForDoctorOnDate(
                request.getDoctorId(), scheduledAt.toLocalDate());

        return schedules.stream()
                .anyMatch(s -> !time.isBefore(s.getStartTime()) && !time.isAfter(s.getEndTime()));
    }
}
