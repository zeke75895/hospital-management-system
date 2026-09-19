package com.example.hospital_management_system.mapper;

import com.example.hospital_management_system.dto.request.MedicalRecordRequest;
import com.example.hospital_management_system.dto.response.MedicalRecordResponse;
import com.example.hospital_management_system.entity.Doctor;
import com.example.hospital_management_system.entity.MedicalRecord;
import com.example.hospital_management_system.entity.Patient;
import org.springframework.stereotype.Component;

@Component
public class MedicalRecordMapper {

    public MedicalRecord toEntity(MedicalRecordRequest request, Patient patient, Doctor doctor) {
        return MedicalRecord.builder()
                .patient(patient)
                .doctor(doctor)
                .visitDate(request.getVisitDate())
                .diagnosis(request.getDiagnosis())
                .notes(request.getNotes())
                .prescription(request.getPrescription())
                .build();
    }

    // Reads record.getPatient()/getDoctor() - must be called within an active transaction/session,
    // since both associations are fetched LAZY.
    public MedicalRecordResponse toResponse(MedicalRecord record) {
        Patient patient = record.getPatient();
        Doctor doctor = record.getDoctor();
        return MedicalRecordResponse.builder()
                .id(record.getId())
                .patientId(patient.getId())
                .patientName(patient.getFirstName() + " " + patient.getLastName())
                .doctorId(doctor.getId())
                .doctorName(doctor.getFirstName() + " " + doctor.getLastName())
                .visitDate(record.getVisitDate())
                .diagnosis(record.getDiagnosis())
                .notes(record.getNotes())
                .prescription(record.getPrescription())
                .build();
    }
}
