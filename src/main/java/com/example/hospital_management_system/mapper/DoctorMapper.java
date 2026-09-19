package com.example.hospital_management_system.mapper;

import com.example.hospital_management_system.dto.request.DoctorRequest;
import com.example.hospital_management_system.dto.response.DoctorResponse;
import com.example.hospital_management_system.entity.Doctor;
import org.springframework.stereotype.Component;

@Component
public class DoctorMapper {

    public Doctor toEntity(DoctorRequest request) {
        return Doctor.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .specialty(request.getSpecialty())
                .email(request.getEmail())
                .licenseNumber(request.getLicenseNumber())
                .build();
    }

    public DoctorResponse toResponse(Doctor doctor) {
        return DoctorResponse.builder()
                .id(doctor.getId())
                .firstName(doctor.getFirstName())
                .lastName(doctor.getLastName())
                .specialty(doctor.getSpecialty())
                .email(doctor.getEmail())
                .licenseNumber(doctor.getLicenseNumber())
                .build();
    }
}
