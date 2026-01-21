package com.application.service;

import com.application.entity.Patient;
import com.application.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class PatientDataCleanupService {

    @Autowired
    private PatientRepository patientRepository;

    public void cleanupDuplicatePatients() {
        System.out.println("Starting cleanup of duplicate patients...");
        
        // Find all patients
        List<Patient> allPatients = patientRepository.findAll();
        System.out.println("Total patients found: " + allPatients.size());
        
        // Group by patientId to find duplicates
        Map<String, List<Patient>> patientsById = allPatients.stream()
                .collect(Collectors.groupingBy(Patient::getPatientId));
        
        int duplicatesFound = 0;
        int duplicatesRemoved = 0;
        
        for (Map.Entry<String, List<Patient>> entry : patientsById.entrySet()) {
            String patientId = entry.getKey();
            List<Patient> patientsWithSameId = entry.getValue();
            
            if (patientsWithSameId.size() > 1) {
                duplicatesFound++;
                System.out.println("Found duplicate Patient ID: " + patientId + " (" + patientsWithSameId.size() + " records)");
                
                // Sort by ID (keep the one with lowest ID - oldest record)
                patientsWithSameId.sort(Comparator.comparing(Patient::getId));
                
                // Keep the first one, delete the rest
                Patient toKeep = patientsWithSameId.get(0);
                List<Patient> toDelete = patientsWithSameId.subList(1, patientsWithSameId.size());
                
                System.out.println("  Keeping: ID=" + toKeep.getId() + ", " + toKeep.getFullName());
                
                for (Patient patient : toDelete) {
                    System.out.println("  Deleting: ID=" + patient.getId() + ", " + patient.getFullName());
                    patientRepository.delete(patient);
                    duplicatesRemoved++;
                }
            }
        }
        
        System.out.println("Cleanup completed. Found " + duplicatesFound + " duplicate groups, removed " + duplicatesRemoved + " duplicate records.");
    }
}
