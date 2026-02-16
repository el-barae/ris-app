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
@Table(name = "procedure_steps")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcedureStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "procedure_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private Procedure procedure;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "step_order")
    private Integer stepOrder;

    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;

    @Column(name = "actual_duration_minutes")
    private Integer actualDurationMinutes;

    @Column(name = "is_required")
    private Boolean isRequired = true;

    @Column(name = "is_completed")
    private Boolean isCompleted = false;

    @Column(columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "completion_notes", columnDefinition = "TEXT")
    private String completionNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Helper methods
    public void markAsCompleted(String notes) {
        this.isCompleted = true;
        this.completionNotes = notes;
    }

    public void markAsIncomplete() {
        this.isCompleted = false;
        this.completionNotes = null;
    }
}
