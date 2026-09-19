package com.example.hospital_management_system.dto.response;

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
public class ScheduleResponse {
    private Long id;
    private Long doctorId;
    private String doctorName;
    private DayOfWeek dayOfWeek;
    private LocalDate specificDate;
    private LocalTime startTime;
    private LocalTime endTime;
}
