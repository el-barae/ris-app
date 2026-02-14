package com.application.repository;

import com.application.entity.Exam;
import com.application.entity.ExamStatus;
import com.application.entity.Order;
import com.application.entity.Hospital;
import com.application.entity.Procedure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    @Query("SELECT e FROM Exam e LEFT JOIN FETCH e.order o LEFT JOIN FETCH o.patient LEFT JOIN FETCH o.doctor LEFT JOIN FETCH e.report LEFT JOIN FETCH e.procedure p LEFT JOIN FETCH p.procedureCatalog")
    List<Exam> findAllWithRelations();
    
    @Query("SELECT e FROM Exam e LEFT JOIN FETCH e.order o LEFT JOIN FETCH o.patient LEFT JOIN FETCH o.doctor LEFT JOIN FETCH e.report LEFT JOIN FETCH e.procedure p LEFT JOIN FETCH p.procedureCatalog WHERE e.id = :id")
    Optional<Exam> findByIdWithRelations(Long id);
    
    @Query("SELECT e FROM Exam e LEFT JOIN FETCH e.order o LEFT JOIN FETCH o.patient LEFT JOIN FETCH o.doctor LEFT JOIN FETCH e.report LEFT JOIN FETCH e.procedure p LEFT JOIN FETCH p.procedureCatalog LEFT JOIN FETCH e.modality LEFT JOIN FETCH e.modality.modalityType WHERE e.status = :status")
    List<Exam> findByStatusWithRelations(ExamStatus status);
    
    @Query("SELECT e FROM Exam e LEFT JOIN FETCH e.order o LEFT JOIN FETCH o.patient LEFT JOIN o.doctor LEFT JOIN FETCH e.report LEFT JOIN e.procedure WHERE o.doctor.id = :medecinId")
    List<Exam> findByMedecinWithRelations(Long medecinId);
    
    @Query("SELECT e FROM Exam e LEFT JOIN FETCH e.order o LEFT JOIN FETCH o.patient LEFT JOIN o.doctor LEFT JOIN FETCH e.report LEFT JOIN e.procedure WHERE e.scheduledDateTime BETWEEN :start AND :end")
    List<Exam> findByScheduledDateTimeBetweenWithRelations(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT e FROM Exam e LEFT JOIN FETCH e.order o LEFT JOIN FETCH o.patient LEFT JOIN o.doctor LEFT JOIN FETCH e.report LEFT JOIN e.procedure WHERE o.patient.id = :patientId")
    List<Exam> findByPatientWithRelations(Long patientId);

    @Query("SELECT e FROM Exam e LEFT JOIN FETCH e.order o LEFT JOIN FETCH o.patient LEFT JOIN o.doctor WHERE e.accessionNumber = :accessionNumber")
    Optional<Exam> findByAccessionNumberWithRelations(String accessionNumber);

    Optional<Exam> findByAccessionNumber(String accessionNumber);
    
    // Nouvelles méthodes pour filtrer par hôpital
    @Query("SELECT e FROM Exam e LEFT JOIN FETCH e.order o LEFT JOIN FETCH o.patient LEFT JOIN FETCH o.doctor LEFT JOIN FETCH e.report LEFT JOIN FETCH e.procedure p LEFT JOIN FETCH p.procedureCatalog WHERE e.status = :status AND o.hospital.id = :hospitalId")
    List<Exam> findByStatusAndHospitalId(@Param("status") ExamStatus status, @Param("hospitalId") Long hospitalId);
    
    @Query("SELECT e FROM Exam e LEFT JOIN FETCH e.order o LEFT JOIN FETCH o.patient LEFT JOIN FETCH o.doctor LEFT JOIN FETCH e.report LEFT JOIN FETCH e.procedure p LEFT JOIN FETCH p.procedureCatalog WHERE o.hospital.id = :hospitalId")
    List<Exam> findByHospitalId(@Param("hospitalId") Long hospitalId);
}
