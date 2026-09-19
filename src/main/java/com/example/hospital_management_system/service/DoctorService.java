package com.example.hospital_management_system.service;

import com.example.hospital_management_system.dto.request.DoctorRequest;
import com.example.hospital_management_system.dto.response.DoctorResponse;
import com.example.hospital_management_system.entity.AppointmentStatus;
import com.example.hospital_management_system.entity.Doctor;
import com.example.hospital_management_system.exception.ResourceNotFoundException;
import com.example.hospital_management_system.mapper.DoctorMapper;
import com.example.hospital_management_system.repository.AppointmentRepository;
import com.example.hospital_management_system.repository.DoctorRepository;
import com.example.hospital_management_system.repository.ScheduleRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorMapper doctorMapper;
    private final AppointmentRepository appointmentRepository;
    private final ScheduleRepository scheduleRepository;

    @Transactional
    public DoctorResponse createDoctor(DoctorRequest request) {
        Doctor saved = doctorRepository.save(doctorMapper.toEntity(request));
        return doctorMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DoctorResponse getDoctor(Long id) {
        return doctorMapper.toResponse(findDoctorOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<DoctorResponse> listDoctors(Pageable pageable) {
        return doctorRepository.findAll(pageable).map(doctorMapper::toResponse);
    }

    @Transactional
    public DoctorResponse updateDoctor(Long id, DoctorRequest request) {
        Doctor doctor = findDoctorOrThrow(id);
        doctor.setFirstName(request.getFirstName());
        doctor.setLastName(request.getLastName());
        doctor.setSpecialty(request.getSpecialty());
        doctor.setEmail(request.getEmail());
        doctor.setLicenseNumber(request.getLicenseNumber());
        return doctorMapper.toResponse(doctor);
    }

    @Transactional
    public void deleteDoctor(Long id) {
        doctorRepository.delete(findDoctorOrThrow(id));
    }

    /**
     * Advisory only: is doctorId both scheduled to work AND free for the entire window
     * [start, end)? This is a plain, unlocked read - fine for "show me open slots" style queries,
     * but it is NOT the booking guard. Two callers could both see "available" here and then race
     * to book the same slot; that race is what AppointmentService.bookAppointment()'s pessimistic
     * locking exists to close. Never wire this method's result directly into a booking decision.
     */
    @Transactional(readOnly = true)
    public boolean isDoctorAvailable(Long doctorId, LocalDateTime start, LocalDateTime end) {
        Doctor doctor = findDoctorOrThrow(doctorId);

        boolean withinScheduleWindow =
                scheduleRepository.findSchedulesForDoctorOnDate(doctor.getId(), start.toLocalDate()).stream()
                        .anyMatch(s -> !start.toLocalTime().isBefore(s.getStartTime())
                                && !end.toLocalTime().isAfter(s.getEndTime()));

        if (!withinScheduleWindow) {
            return false;
        }

        return appointmentRepository.findByDoctorIdAndScheduledAtBetween(doctor.getId(), start, end).stream()
                .noneMatch(a -> a.getStatus() == AppointmentStatus.BOOKED);
    }

    private Doctor findDoctorOrThrow(Long id) {
        return doctorRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor not found: " + id));
    }
}
