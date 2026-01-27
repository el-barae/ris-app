package com.application.repository;

import com.application.entity.ModalityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModalityTypeRepository extends JpaRepository<ModalityType, Long> {

    List<ModalityType> findByIsActive(Boolean isActive);

    List<ModalityType> findByIsActiveOrderBySortOrderAsc(Boolean isActive);

    Optional<ModalityType> findByCode(String code);

    Optional<ModalityType> findByCodeAndIsActive(String code, Boolean isActive);

    Optional<ModalityType> findByDicomCode(String dicomCode);

    @Query("SELECT m FROM ModalityType m WHERE " +
           "(:name IS NULL OR LOWER(m.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:code IS NULL OR LOWER(m.code) LIKE LOWER(CONCAT('%', :code, '%'))) AND " +
           "m.isActive = true")
    List<ModalityType> searchModalities(@Param("name") String name, @Param("code") String code);

    @Query("SELECT DISTINCT m.code FROM ModalityType m WHERE m.isActive = true ORDER BY m.sortOrder ASC, m.code ASC")
    List<String> findAllActiveCodes();

    @Query("SELECT m FROM ModalityType m WHERE m.isActive = true ORDER BY m.sortOrder ASC, m.code ASC")
    List<ModalityType> findAllActiveOrdered();
}
