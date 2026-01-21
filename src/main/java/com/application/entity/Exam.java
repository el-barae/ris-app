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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExamType examType;

    @Column(name = "modality", nullable = false)
    private String modality;

    @Column(name = "scheduled_date_time", nullable = false)
    private LocalDateTime scheduledDateTime;

    @Column(name = "performed_date_time")
    private LocalDateTime performedDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExamStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Column(columnDefinition = "TEXT")
    private String instructions;

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
    public void generateAccessionNumberAndCalculateModality() {
        if (accessionNumber == null || accessionNumber.isEmpty()) {
            long timestamp = System.currentTimeMillis();
            String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            accessionNumber = "ACC" + timestamp + random;
        }
        
        if (examType != null) {
            switch (examType) {
                case CT:
                    modality = "CT";
                    break;
                case MRI:
                    modality = "MR";
                    break;
                case RX:
                    modality = "XR";
                    break;
                case ECHO:
                    modality = "US";
                    break;
                case MAMMO:
                    modality = "MG";
                    break;
                case FLUORO:
                    modality = "RF";
                    break;
                case PET:
                    modality = "PT";
                    break;
                default:
                    modality = examType.name();
            }
        }
    }
}
