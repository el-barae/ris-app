package com.application.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "procedures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Procedure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "procedure_code")
    private String procedureCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modality_type_id")
    @EqualsAndHashCode.Exclude
    private ModalityType modalityType;

    @Column
    private String region;

    @Column
    private String laterality;

    @Column(name = "contrast_required")
    private Boolean contrastRequired = false;

    @Column(name = "contrast_type")
    private String contrastType;

    @Column(name = "injection_rate")
    private Double injectionRate;

    @Column(name = "injection_volume")
    private Double injectionVolume;

    @Column(name = "delay_seconds")
    private Integer delaySeconds;

    @Column(columnDefinition = "TEXT")
    private String preparationInstructions;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_emergency")
    private Boolean isEmergency = false;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "TEXT")
    private String specialInstructions;

    @Column(name = "scheduled_duration_minutes")
    private Integer scheduledDurationMinutes;

    @Column(name = "actual_duration_minutes")
    private Integer actualDurationMinutes;

    // Relation OneToMany avec Exam
    @OneToMany(mappedBy = "procedure", cascade = CascadeType.ALL)
    @EqualsAndHashCode.Exclude
    private List<Exam> exams = new ArrayList<>();

    // Relation OneToOne avec ProcedureCatalog (template)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procedure_catalog_id")
    @EqualsAndHashCode.Exclude
    private ProcedureCatalog procedureCatalog;

    // Relation OneToMany avec ProcedureStep
    @OneToMany(mappedBy = "procedure", cascade = CascadeType.ALL, orphanRemoval = true)
    @EqualsAndHashCode.Exclude
    private List<ProcedureStep> procedureSteps = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Méthodes utilitaires pour accéder aux données du catalogue
    public String getModalityCode() {
        return modalityType != null ? modalityType.getCode() : 
               (procedureCatalog != null && procedureCatalog.getModalityType() != null ? 
                procedureCatalog.getModalityType().getCode() : null);
    }

    public String getModalityName() {
        return modalityType != null ? modalityType.getName() : 
               (procedureCatalog != null && procedureCatalog.getModalityType() != null ? 
                procedureCatalog.getModalityType().getName() : null);
    }

    // Helper methods for managing procedure steps
    public void addProcedureStep(ProcedureStep step) {
        if (!procedureSteps.contains(step)) {
            procedureSteps.add(step);
            step.setProcedure(this);
        }
    }

    public void removeProcedureStep(ProcedureStep step) {
        if (procedureSteps.contains(step)) {
            procedureSteps.remove(step);
            step.setProcedure(null);
        }
    }

    // Helper methods for managing exams
    public void addExam(Exam exam) {
        if (!exams.contains(exam)) {
            exams.add(exam);
            exam.setProcedure(this);
        }
    }

    public void removeExam(Exam exam) {
        if (exams.contains(exam)) {
            exams.remove(exam);
            exam.setProcedure(null);
        }
    }

    public List<ProcedureStep> getCompletedSteps() {
        return procedureSteps.stream()
                .filter(step -> Boolean.TRUE.equals(step.getIsCompleted()))
                .sorted((s1, s2) -> Integer.compare(s1.getStepOrder(), s2.getStepOrder()))
                .toList();
    }

    public List<ProcedureStep> getPendingSteps() {
        return procedureSteps.stream()
                .filter(step -> !Boolean.TRUE.equals(step.getIsCompleted()))
                .sorted((s1, s2) -> Integer.compare(s1.getStepOrder(), s2.getStepOrder()))
                .toList();
    }

    public boolean isFullyCompleted() {
        return procedureSteps.stream()
                .allMatch(step -> !Boolean.TRUE.equals(step.getIsRequired()) || 
                                 Boolean.TRUE.equals(step.getIsCompleted()));
    }
}
