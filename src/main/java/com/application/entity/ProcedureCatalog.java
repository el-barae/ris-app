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
import java.util.List;

@Entity
@Table(name = "procedure_catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcedureCatalog {

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
    private String injectionRate;

    @Column(name = "contrast_volume")
    private String contrastVolume;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String additionalInstructions;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "procedure", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    private List<Exam> exams;
}
