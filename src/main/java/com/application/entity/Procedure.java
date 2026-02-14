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

    // Relation OneToOne avec Exam
    @OneToOne(fetch = FetchType.LAZY, mappedBy = "procedure")
    @EqualsAndHashCode.Exclude
    private Exam exam;

    // Relation OneToOne avec ProcedureCatalog (template)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procedure_catalog_id")
    @EqualsAndHashCode.Exclude
    private ProcedureCatalog procedureCatalog;

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
}
