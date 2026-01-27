package com.application.repository;

import com.application.entity.Exam;
import com.application.entity.ExamStatus;
import com.application.entity.Patient;
import com.application.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    @Query("SELECT e FROM Exam e LEFT JOIN FETCH e.patient LEFT JOIN FETCH e.medecin LEFT JOIN FETCH e.report LEFT JOIN FETCH e.procedure")
    List<Exam> findAllWithRelations();
    
    @Query("SELECT e FROM Exam e LEFT JOIN FETCH e.patient LEFT JOIN FETCH e.medecin LEFT JOIN FETCH e.report LEFT JOIN FETCH e.procedure WHERE e.id = :id")
    Optional<Exam> findByIdWithRelations(Long id);
    
    @Query("SELECT e FROM Exam e LEFT JOIN FETCH e.patient LEFT JOIN FETCH e.medecin LEFT JOIN FETCH e.report LEFT JOIN FETCH e.procedure WHERE e.status = :status")
    List<Exam> findByStatusWithRelations(ExamStatus status);
    
    @Query("SELECT e FROM Exam e LEFT JOIN FETCH e.patient LEFT JOIN FETCH e.medecin LEFT JOIN FETCH e.report LEFT JOIN FETCH e.procedure WHERE e.medecin.id = :medecinId")
    List<Exam> findByMedecinWithRelations(Long medecinId);
    
    @Query("SELECT e FROM Exam e LEFT JOIN FETCH e.patient LEFT JOIN FETCH e.medecin LEFT JOIN FETCH e.report LEFT JOIN FETCH e.procedure WHERE e.scheduledDateTime BETWEEN :start AND :end")
    List<Exam> findByScheduledDateTimeBetweenWithRelations(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT e FROM Exam e LEFT JOIN FETCH e.patient LEFT JOIN FETCH e.medecin LEFT JOIN FETCH e.report LEFT JOIN FETCH e.procedure WHERE e.patient.id = :patientId")
    List<Exam> findByPatientWithRelations(Long patientId);

    @Query("SELECT e FROM Exam e LEFT JOIN FETCH e.patient LEFT JOIN FETCH e.medecin WHERE e.accessionNumber = :accessionNumber")
    Optional<Exam> findByAccessionNumberWithRelations(String accessionNumber);

    List<Exam> findByPatient(Patient patient);

    List<Exam> findByStatus(ExamStatus status);

    List<Exam> findByMedecin(User medecin);

    List<Exam> findByScheduledDateTimeBetween(LocalDateTime start, LocalDateTime end);

    Optional<Exam> findByAccessionNumber(String accessionNumber);
}
