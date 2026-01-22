package com.application.service;

import com.application.entity.Patient;

import java.util.List;

public interface PatientService {

    Patient createPatient(Patient patient);

    Patient updatePatient(Long id, Patient patient);

    void deletePatient(Long id);

    Patient findById(Long id);

    Patient findByPatientId(String patientId);

    List<Patient> findAll();

    List<Patient> searchPatients(String searchTerm);

    String generatePatientId();

    List<Object[]> findDuplicatePatientIds();

    int cleanupGenderOtherValues();
}
