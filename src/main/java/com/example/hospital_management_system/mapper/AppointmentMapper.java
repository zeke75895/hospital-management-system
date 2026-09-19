package com.example.hospital_management_system.mapper;

import com.example.hospital_management_system.dto.request.AppointmentRequest;
import com.example.hospital_management_system.dto.response.AppointmentResponse;
import com.example.hospital_management_system.entity.Appointment;
import com.example.hospital_management_system.entity.AppointmentStatus;
import com.example.hospital_management_system.entity.Doctor;
import com.example.hospital_management_system.entity.Patient;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

    // Patient/doctor are resolved by the caller (service layer); status always starts as BOOKED
    // here, since AppointmentRequest has no status field for a client to set directly.
    public Appointment toEntity(AppointmentRequest request, Patient patient, Doctor doctor) {
        return Appointment.builder()
                .patient(patient)
                .doctor(doctor)
                .scheduledAt(request.getScheduledAt())
                .status(AppointmentStatus.BOOKED)
                .reason(request.getReason())
                .build();
    }

    // Reads appointment.getPatient()/getDoctor() - must be called within an active
    // transaction/session, since both associations are fetched LAZY.
    public AppointmentResponse toResponse(Appointment appointment) {
        Patient patient = appointment.getPatient();
        Doctor doctor = appointment.getDoctor();
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .patientId(patient.getId())
                .patientName(patient.getFirstName() + " " + patient.getLastName())
                .doctorId(doctor.getId())
                .doctorName(doctor.getFirstName() + " " + doctor.getLastName())
                .scheduledAt(appointment.getScheduledAt())
                .status(appointment.getStatus())
                .reason(appointment.getReason())
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .build();
    }
}
