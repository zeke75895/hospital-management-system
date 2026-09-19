package com.example.hospital_management_system.service;

import com.example.hospital_management_system.dto.request.ScheduleRequest;
import com.example.hospital_management_system.dto.response.ScheduleResponse;
import com.example.hospital_management_system.entity.Doctor;
import com.example.hospital_management_system.entity.Schedule;
import com.example.hospital_management_system.exception.ResourceNotFoundException;
import com.example.hospital_management_system.mapper.ScheduleMapper;
import com.example.hospital_management_system.repository.DoctorRepository;
import com.example.hospital_management_system.repository.ScheduleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final DoctorRepository doctorRepository;
    private final ScheduleMapper scheduleMapper;

    @Transactional
    public ScheduleResponse createSchedule(Long doctorId, ScheduleRequest request) {
        Doctor doctor = findDoctorOrThrow(doctorId);
        Schedule saved = scheduleRepository.save(scheduleMapper.toEntity(request, doctor));
        return scheduleMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ScheduleResponse getSchedule(Long id) {
        return scheduleMapper.toResponse(findScheduleOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> listSchedulesForDoctor(Long doctorId) {
        return scheduleRepository.findByDoctorId(doctorId).stream()
                .map(scheduleMapper::toResponse)
                .toList();
    }

    @Transactional
    public ScheduleResponse updateSchedule(Long id, Long doctorId, ScheduleRequest request) {
        Schedule schedule = findScheduleOrThrow(id);
        Doctor doctor = findDoctorOrThrow(doctorId);
        schedule.setDoctor(doctor);
        schedule.setDayOfWeek(request.getDayOfWeek());
        schedule.setSpecificDate(request.getSpecificDate());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        return scheduleMapper.toResponse(schedule);
    }

    @Transactional
    public void deleteSchedule(Long id) {
        scheduleRepository.delete(findScheduleOrThrow(id));
    }

    private Schedule findScheduleOrThrow(Long id) {
        return scheduleRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found: " + id));
    }

    private Doctor findDoctorOrThrow(Long doctorId) {
        return doctorRepository
                .findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + doctorId));
    }
}
