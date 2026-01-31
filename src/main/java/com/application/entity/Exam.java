package com.application.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "exams")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String accessionNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medecin_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private User medecin;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "modality_type_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private ModalityType modalityType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "modality_id")
    @EqualsAndHashCode.Exclude
    private Modality modality;

    @Column(name = "scheduled_date_time")
    private LocalDateTime scheduledDateTime;

    @Column(name = "performed_date_time")
    private LocalDateTime performedDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExamStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procedure_id")
    @EqualsAndHashCode.Exclude
    private ProcedureCatalog procedure;

    @Column(columnDefinition = "TEXT")
    private String additionalInstructions;

    @Column(name = "study_instance_uid")
    private String studyInstanceUID;

    @OneToOne(mappedBy = "exam", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    private Report report;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void generateAccessionNumber() {
        if (accessionNumber == null || accessionNumber.isEmpty()) {
            long timestamp = System.currentTimeMillis();
            String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            accessionNumber = "ACC" + timestamp + random;
        }
    }

    // Méthode utilitaire pour obtenir le code de modalité
    public String getModalityCode() {
        return modalityType != null ? modalityType.getCode() : null;
    }

    // Méthode pour accéder directement à l'entité Modality (non dépréciée)
    public Modality getModalityEntity() {
        return modality;
    }

    // Méthode pour définir directement l'entité Modality (non dépréciée)
    public void setModalityEntity(Modality modality) {
        this.modality = modality;
        // Mettre à jour automatiquement le modalityType si nécessaire
        if (modality != null && modality.getModalityType() != null) {
            this.modalityType = modality.getModalityType();
        }
    }

    // Méthodes de compatibilité pour ne pas casser le code existant
    @Deprecated
    public String getModality() {
        return getModalityCode();
    }

    @Deprecated
    public void setModality(String modality) {
        // Cette méthode ne fait rien pour la compatibilité
        // Utiliser setModalityType() à la place
    }

    @Deprecated
    public ExamType getExamType() {
        // Retourne null car examType est supprimé
        return null;
    }

    @Deprecated
    public void setExamType(ExamType examType) {
        // Cette méthode ne fait rien pour la compatibilité
        // Utiliser setModalityType() à la place
    }
}
