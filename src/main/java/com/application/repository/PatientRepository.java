package com.application.repository;

import com.application.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByPatientId(String patientId);

    List<Patient> findByLastNameContainingIgnoreCase(String lastName);

    List<Patient> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String firstName, String lastName);

    @Query("SELECT p.patientId, COUNT(p) as count FROM Patient p GROUP BY p.patientId HAVING COUNT(p) > 1")
    List<Object[]> findDuplicatePatientIds();

    @Modifying
    @Transactional
    @Query("UPDATE Patient p SET p.gender = 'MALE' WHERE p.gender = 'OTHER'")
    int updateGenderOtherToMale();
}
