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
import java.util.UUID;

@Entity
@Table(name = "exams")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Exam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String accessionNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private Order order;

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
    private Procedure procedure;

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

    // Méthodes pour accéder au patient et médecin via l'ordre
    public Patient getPatient() {
        return order != null ? order.getPatient() : null;
    }

    public User getMedecin() {
        return order != null ? order.getDoctor() : null;
    }

    // Méthodes pour la procédure
    public Procedure getProcedure() {
        return procedure;
    }

    public void setProcedure(Procedure procedure) {
        this.procedure = procedure;
    }

    // Méthodes de compatibilité pour ne pas casser le code existant
    public void setPatient(Patient patient) {
        // Ne fait rien - le patient est défini via l'ordre
    }

    public void setMedecin(User medecin) {
        // Ne fait rien - le médecin est défini via l'ordre
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
