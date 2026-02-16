package com.application.repository;

import com.application.entity.ProcedureStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcedureStepRepository extends JpaRepository<ProcedureStep, Long> {

    List<ProcedureStep> findByProcedureIdOrderByStepOrder(Long procedureId);

    List<ProcedureStep> findByProcedureIdAndIsCompletedOrderByStepOrder(Long procedureId, Boolean isCompleted);

    List<ProcedureStep> findByProcedureIdAndIsRequiredOrderByStepOrder(Long procedureId, Boolean isRequired);

    @Query("SELECT ps FROM ProcedureStep ps WHERE ps.procedure.id = :procedureId ORDER BY ps.stepOrder")
    List<ProcedureStep> findStepsByProcedureId(@Param("procedureId") Long procedureId);

    @Query("SELECT COUNT(ps) FROM ProcedureStep ps WHERE ps.procedure.id = :procedureId AND ps.isCompleted = true")
    long countCompletedStepsByProcedureId(@Param("procedureId") Long procedureId);

    @Query("SELECT COUNT(ps) FROM ProcedureStep ps WHERE ps.procedure.id = :procedureId AND ps.isRequired = true")
    long countRequiredStepsByProcedureId(@Param("procedureId") Long procedureId);
}
