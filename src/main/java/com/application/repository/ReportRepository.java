package com.application.repository;

import com.application.entity.Exam;
import com.application.entity.Report;
import com.application.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("SELECT r FROM Report r LEFT JOIN FETCH r.exam e LEFT JOIN FETCH e.order o LEFT JOIN FETCH o.patient LEFT JOIN FETCH o.doctor LEFT JOIN FETCH r.radiologue")
    List<Report> findAllWithRelations();
    
    @Query("SELECT r FROM Report r LEFT JOIN FETCH r.exam e LEFT JOIN FETCH e.order o LEFT JOIN FETCH o.patient LEFT JOIN FETCH o.doctor LEFT JOIN FETCH r.radiologue WHERE r.id = :id")
    Optional<Report> findByIdWithRelations(Long id);
    
    @Query("SELECT r FROM Report r LEFT JOIN FETCH r.exam e LEFT JOIN FETCH e.order o LEFT JOIN FETCH o.patient LEFT JOIN FETCH o.doctor LEFT JOIN FETCH r.radiologue WHERE e.id = :examId")
    Optional<Report> findByExamWithRelations(Long examId);
    
    @Query("SELECT r FROM Report r LEFT JOIN FETCH r.exam e LEFT JOIN FETCH e.order o LEFT JOIN FETCH o.patient LEFT JOIN FETCH o.doctor LEFT JOIN FETCH r.radiologue WHERE r.radiologue.id = :radiologueId")
    List<Report> findByRadiologueWithRelations(Long radiologueId);
    
    @Query("SELECT r FROM Report r LEFT JOIN FETCH r.exam e LEFT JOIN FETCH e.order o LEFT JOIN FETCH o.patient LEFT JOIN FETCH o.doctor LEFT JOIN FETCH r.radiologue WHERE r.validated = false")
    List<Report> findUnvalidatedWithRelations();

    Optional<Report> findByExam(Exam exam);

    List<Report> findByRadiologue(User radiologue);

    List<Report> findByValidatedFalse();
}
