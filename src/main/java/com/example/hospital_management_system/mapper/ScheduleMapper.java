package com.example.hospital_management_system.mapper;

import com.example.hospital_management_system.dto.request.ScheduleRequest;
import com.example.hospital_management_system.dto.response.ScheduleResponse;
import com.example.hospital_management_system.entity.Doctor;
import com.example.hospital_management_system.entity.Schedule;
import org.springframework.stereotype.Component;

@Component
public class ScheduleMapper {

    // Takes the already-resolved Doctor rather than looking it up itself: resolving doctorId ->
    // Doctor (and checking it exists) is a service/repository concern, not a mapper concern.
    public Schedule toEntity(ScheduleRequest request, Doctor doctor) {
        return Schedule.builder()
                .doctor(doctor)
                .dayOfWeek(request.getDayOfWeek())
                .specificDate(request.getSpecificDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();
    }

    // Reads schedule.getDoctor() - must be called within an active transaction/session, since
    // Schedule.doctor is fetched LAZY.
    public ScheduleResponse toResponse(Schedule schedule) {
        Doctor doctor = schedule.getDoctor();
        return ScheduleResponse.builder()
                .id(schedule.getId())
                .doctorId(doctor.getId())
                .doctorName(doctor.getFirstName() + " " + doctor.getLastName())
                .dayOfWeek(schedule.getDayOfWeek())
                .specificDate(schedule.getSpecificDate())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .build();
    }
}
