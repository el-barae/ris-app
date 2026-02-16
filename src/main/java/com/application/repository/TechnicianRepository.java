package com.application.repository;

import com.application.entity.Technician;
import com.application.entity.ModalityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TechnicianRepository extends JpaRepository<Technician, Long> {

    List<Technician> findByIsActive(Boolean isActive);

    List<Technician> findByModalityTypesContaining(ModalityType modalityType);

    @Query("SELECT t FROM Technician t JOIN FETCH t.modalityTypes WHERE t.isActive = true ORDER BY t.lastName, t.firstName")
    List<Technician> findAllActiveWithModalityTypes();

    @Query("SELECT t FROM Technician t JOIN FETCH t.modalityTypes WHERE t.isActive = true AND :modalityType MEMBER OF t.modalityTypes")
    List<Technician> findByModalityTypeAndActive(@Param("modalityType") ModalityType modalityType);

    boolean existsByEmployeeId(String employeeId);
}
