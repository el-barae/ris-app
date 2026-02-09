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
@Table(name = "schedule_slots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scheduled_start_time", nullable = false)
    private LocalDateTime scheduledStartTime;

    @Column(name = "scheduled_end_time", nullable = false)
    private LocalDateTime scheduledEndTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleSlotStatus status = ScheduleSlotStatus.SCHEDULED;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_line_id", nullable = false, unique = true)
    @EqualsAndHashCode.Exclude
    private Exam orderLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modality_resource_id", nullable = false)
    @EqualsAndHashCode.Exclude
    private Modality modalityResource;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
