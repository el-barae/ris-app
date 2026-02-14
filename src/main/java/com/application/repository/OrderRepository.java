package com.application.repository;

import com.application.entity.Order;
import com.application.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByAccessionNumber(String accessionNumber);
    
    Optional<Order> findByStudyInstanceUID(String studyInstanceUID);
    
    List<Order> findByHospitalId(Long hospitalId);
    
    List<Order> findByDoctorId(Long doctorId);
    
    List<Order> findByPatientId(Long patientId);
    
    @Query("SELECT o FROM Order o WHERE o.hospital.id = :hospitalId AND o.doctor.id = :doctorId")
    List<Order> findByHospitalAndDoctor(@Param("hospitalId") Long hospitalId, @Param("doctorId") Long doctorId);
    
    @Query("SELECT o FROM Order o JOIN o.exams e WHERE e.id = :examId")
    Optional<Order> findByExamId(@Param("examId") Long examId);
    
    boolean existsByAccessionNumber(String accessionNumber);
    
    boolean existsByStudyInstanceUID(String studyInstanceUID);
    
    @Query("SELECT COUNT(o) FROM Order o WHERE o.patient.id = :patientId")
    long countByPatientId(@Param("patientId") Long patientId);
}
