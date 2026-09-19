package com.example.hospital_management_system.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleRequest {

    @NotNull
    private Long doctorId;

    private DayOfWeek dayOfWeek;

    private LocalDate specificDate;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    @AssertTrue(message = "Exactly one of dayOfWeek or specificDate must be set, not both or neither")
    public boolean isExactlyOneOfDayOrDateSet() {
        return (dayOfWeek == null) != (specificDate == null);
    }

    @AssertTrue(message = "startTime must be before endTime")
    public boolean isTimeRangeValid() {
        return startTime == null || endTime == null || startTime.isBefore(endTime);
    }
}
