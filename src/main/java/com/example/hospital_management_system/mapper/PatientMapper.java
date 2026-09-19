package com.example.hospital_management_system.mapper;

import com.example.hospital_management_system.dto.request.PatientRequest;
import com.example.hospital_management_system.dto.response.PatientResponse;
import com.example.hospital_management_system.entity.Patient;
import org.springframework.stereotype.Component;

@Component
public class PatientMapper {

    public Patient toEntity(PatientRequest request) {
        return Patient.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .dateOfBirth(request.getDateOfBirth())
                .email(request.getEmail())
                .phone(request.getPhone())
                .medicalRecordNumber(request.getMedicalRecordNumber())
                .build();
    }

    public PatientResponse toResponse(Patient patient) {
        return PatientResponse.builder()
                .id(patient.getId())
                .firstName(patient.getFirstName())
                .lastName(patient.getLastName())
                .dateOfBirth(patient.getDateOfBirth())
                .email(patient.getEmail())
                .phone(patient.getPhone())
                .medicalRecordNumber(patient.getMedicalRecordNumber())
                .createdAt(patient.getCreatedAt())
                .build();
    }
}
