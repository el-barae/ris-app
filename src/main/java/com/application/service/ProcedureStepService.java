package com.application.service;

import com.application.entity.ProcedureStep;
import com.application.repository.ProcedureStepRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProcedureStepService {

    @Autowired
    private ProcedureStepRepository procedureStepRepository;

    public List<ProcedureStep> findAll() {
        return procedureStepRepository.findAll();
    }

    public ProcedureStep findById(Long id) {
        return procedureStepRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ProcedureStep not found with id: " + id));
    }

    public List<ProcedureStep> findByProcedureId(Long procedureId) {
        return procedureStepRepository.findStepsByProcedureId(procedureId);
    }

    public List<ProcedureStep> findCompletedStepsByProcedureId(Long procedureId) {
        return procedureStepRepository.findByProcedureIdAndIsCompletedOrderByStepOrder(procedureId, true);
    }

    public List<ProcedureStep> findPendingStepsByProcedureId(Long procedureId) {
        return procedureStepRepository.findByProcedureIdAndIsCompletedOrderByStepOrder(procedureId, false);
    }

    public ProcedureStep save(ProcedureStep procedureStep) {
        return procedureStepRepository.save(procedureStep);
    }

    public ProcedureStep createProcedureStep(ProcedureStep procedureStep) {
        return procedureStepRepository.save(procedureStep);
    }

    public ProcedureStep updateProcedureStep(Long id, ProcedureStep procedureStep) {
        ProcedureStep existingStep = findById(id);
        existingStep.setName(procedureStep.getName());
        existingStep.setDescription(procedureStep.getDescription());
        existingStep.setStepOrder(procedureStep.getStepOrder());
        existingStep.setEstimatedDurationMinutes(procedureStep.getEstimatedDurationMinutes());
        existingStep.setActualDurationMinutes(procedureStep.getActualDurationMinutes());
        existingStep.setIsRequired(procedureStep.getIsRequired());
        existingStep.setInstructions(procedureStep.getInstructions());
        
        return procedureStepRepository.save(existingStep);
    }

    public void deleteProcedureStep(Long id) {
        ProcedureStep procedureStep = findById(id);
        procedureStepRepository.delete(procedureStep);
    }

    public void markStepAsCompleted(Long id, String completionNotes) {
        ProcedureStep step = findById(id);
        step.markAsCompleted(completionNotes);
        procedureStepRepository.save(step);
    }

    public void markStepAsIncomplete(Long id) {
        ProcedureStep step = findById(id);
        step.markAsIncomplete();
        procedureStepRepository.save(step);
    }

    public long countCompletedStepsByProcedureId(Long procedureId) {
        return procedureStepRepository.countCompletedStepsByProcedureId(procedureId);
    }

    public long countRequiredStepsByProcedureId(Long procedureId) {
        return procedureStepRepository.countRequiredStepsByProcedureId(procedureId);
    }

    public boolean isProcedureFullyCompleted(Long procedureId) {
        long completedSteps = countCompletedStepsByProcedureId(procedureId);
        long requiredSteps = countRequiredStepsByProcedureId(procedureId);
        return completedSteps >= requiredSteps;
    }
}
