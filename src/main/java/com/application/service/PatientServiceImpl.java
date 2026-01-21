package com.application.service;

import com.application.entity.Patient;
import com.application.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Random;

@Service
@Transactional
public class PatientServiceImpl implements PatientService {

    @Autowired
    private PatientRepository patientRepository;

    private final Random random = new Random();

    @Override
    public Patient createPatient(Patient patient) {
        // Validation des données
        validatePatientData(patient);
        
        // Génération automatique du patientId si non fourni
        if (patient.getPatientId() == null || patient.getPatientId().trim().isEmpty()) {
            patient.setPatientId(generatePatientId());
        } else {
            // Vérification que le patientId est unique
            if (patientRepository.findByPatientId(patient.getPatientId()).isPresent()) {
                throw new IllegalArgumentException("Patient ID already exists: " + patient.getPatientId());
            }
        }

        return patientRepository.save(patient);
    }

    @Override
    public Patient updatePatient(Long id, Patient patient) {
        Patient existingPatient = findById(id);
        
        // Validation des données
        validatePatientData(patient);
        
        // Mise à jour des champs
        existingPatient.setFirstName(patient.getFirstName());
        existingPatient.setLastName(patient.getLastName());
        existingPatient.setDateOfBirth(patient.getDateOfBirth());
        existingPatient.setGender(patient.getGender());
        existingPatient.setPhone(patient.getPhone());
        existingPatient.setEmail(patient.getEmail());
        existingPatient.setAddress(patient.getAddress());
        existingPatient.setCity(patient.getCity());
        existingPatient.setPostalCode(patient.getPostalCode());

        return patientRepository.save(existingPatient);
    }

    @Override
    public void deletePatient(Long id) {
        if (!patientRepository.existsById(id)) {
            throw new PatientNotFoundException("Patient not found with id: " + id);
        }
        patientRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Patient findById(Long id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Patient findByPatientId(String patientId) {
        return patientRepository.findByPatientId(patientId)
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with patientId: " + patientId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> findAll() {
        return patientRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Patient> searchPatients(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return findAll();
        }
        return patientRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
                searchTerm.trim(), searchTerm.trim());
    }

    @Override
    public String generatePatientId() {
        String patientId;
        int maxAttempts = 100;
        int attempts = 0;
        
        do {
            patientId = "P" + String.format("%08d", random.nextInt(100000000));
            attempts++;
            
            if (attempts > maxAttempts) {
                throw new RuntimeException("Failed to generate unique patient ID after " + maxAttempts + " attempts");
            }
        } while (patientRepository.findByPatientId(patientId).isPresent());
        
        return patientId;
    }

    @Override
    public List<Object[]> findDuplicatePatientIds() {
        return patientRepository.findDuplicatePatientIds();
    }

    private void validatePatientData(Patient patient) {
        // Validation du nom et prénom
        if (patient.getFirstName() == null || patient.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be null or empty");
        }
        if (patient.getLastName() == null || patient.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be null or empty");
        }

        // Validation de la date de naissance
        if (patient.getDateOfBirth() == null) {
            throw new IllegalArgumentException("Date of birth cannot be null");
        }
        
        LocalDate today = LocalDate.now();
        if (patient.getDateOfBirth().isAfter(today)) {
            throw new IllegalArgumentException("Date of birth cannot be in the future");
        }
        
        // Validation de l'âge (< 150 ans)
        int age = Period.between(patient.getDateOfBirth(), today).getYears();
        if (age > 150) {
            throw new IllegalArgumentException("Patient age cannot exceed 150 years");
        }

        // Validation du genre
        if (patient.getGender() == null) {
            throw new IllegalArgumentException("Gender cannot be null");
        }
    }
}
