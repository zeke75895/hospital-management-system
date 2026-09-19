package com.example.hospital_management_system.service;

import com.example.hospital_management_system.dto.request.PatientRequest;
import com.example.hospital_management_system.dto.response.AppointmentResponse;
import com.example.hospital_management_system.dto.response.PatientResponse;
import com.example.hospital_management_system.entity.Patient;
import com.example.hospital_management_system.exception.ResourceNotFoundException;
import com.example.hospital_management_system.mapper.AppointmentMapper;
import com.example.hospital_management_system.mapper.PatientMapper;
import com.example.hospital_management_system.repository.AppointmentRepository;
import com.example.hospital_management_system.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;
    // Read-only access to appointment history lives here rather than going through
    // AppointmentService, to avoid a two-way dependency between the two services (AppointmentService
    // will need Patient/Doctor lookups of its own) for what is just a straightforward read query.
    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper appointmentMapper;

    @Transactional
    public PatientResponse createPatient(PatientRequest request) {
        Patient saved = patientRepository.save(patientMapper.toEntity(request));
        return patientMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PatientResponse getPatient(Long id) {
        return patientMapper.toResponse(findPatientOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<PatientResponse> listPatients(Pageable pageable) {
        return patientRepository.findAll(pageable).map(patientMapper::toResponse);
    }

    @Transactional
    public PatientResponse updatePatient(Long id, PatientRequest request) {
        Patient patient = findPatientOrThrow(id);
        patient.setFirstName(request.getFirstName());
        patient.setLastName(request.getLastName());
        patient.setDateOfBirth(request.getDateOfBirth());
        patient.setEmail(request.getEmail());
        patient.setPhone(request.getPhone());
        patient.setMedicalRecordNumber(request.getMedicalRecordNumber());
        // No explicit save() call: `patient` is a managed entity for the life of this
        // @Transactional method, so Hibernate's dirty checking flushes these field changes in the
        // UPDATE statement it issues at commit - calling save() here would be redundant.
        return patientMapper.toResponse(patient);
    }

    @Transactional
    public void deletePatient(Long id) {
        patientRepository.delete(findPatientOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getPatientHistory(Long patientId, Pageable pageable) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient not found: " + patientId);
        }
        return appointmentRepository
                .findByPatientIdOrderByScheduledAtDesc(patientId, pageable)
                .map(appointmentMapper::toResponse);
    }

    private Patient findPatientOrThrow(Long id) {
        return patientRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + id));
    }
}
