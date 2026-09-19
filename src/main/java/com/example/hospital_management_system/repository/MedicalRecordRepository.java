package com.example.hospital_management_system.repository;

import com.example.hospital_management_system.entity.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {}
