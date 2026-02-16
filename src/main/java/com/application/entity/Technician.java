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
@Table(name = "technicians")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Technician {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false, unique = true, length = 20)
    private String employeeId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "specialization", length = 100)
    private String specialization;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @EqualsAndHashCode.Exclude
    private User user;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "technician_modality_types",
        joinColumns = @JoinColumn(name = "technician_id"),
        inverseJoinColumns = @JoinColumn(name = "modality_type_id")
    )
    @EqualsAndHashCode.Exclude
    private List<ModalityType> modalityTypes;

    @OneToMany(mappedBy = "technician", fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    private List<ScheduleSlot> scheduleSlots;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
