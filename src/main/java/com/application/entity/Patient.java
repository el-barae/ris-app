package com.application.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "patients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(unique = true, nullable = false)
    @EqualsAndHashCode.Include
    private String patientId;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    private String phone;

    private String email;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String city;

    private String postalCode;

    @Column(name = "cin")
    private String cin;

    @Column(name = "passport_number")
    private String passportNumber;

    private String nationality;

    private String parentFirstName;

    private String parentLastName;

    private String parentPhone;

    private String parentRelationship;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Exam> exams;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getParentFullName() {
        if (parentFirstName != null && parentLastName != null) {
            return parentFirstName + " " + parentLastName;
        }
        return null;
    }

    public boolean hasParentInfo() {
        return parentFirstName != null || parentLastName != null || parentPhone != null;
    }

    public boolean hasPassport() {
        return passportNumber != null && !passportNumber.trim().isEmpty();
    }

    public String getMainIdentity() {
        if (hasPassport()) {
            return "Passeport: " + passportNumber;
        } else if (cin != null && !cin.trim().isEmpty()) {
            return "CIN: " + cin;
        }
        return "Non spécifié";
    }
}
