package com.application.repository;

import com.application.entity.ModalityType;
import com.application.entity.ProcedureCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcedureCatalogRepository extends JpaRepository<ProcedureCatalog, Long> {

    List<ProcedureCatalog> findByIsActive(Boolean isActive);

    List<ProcedureCatalog> findByModalityType(ModalityType modalityType);

    List<ProcedureCatalog> findByModalityTypeAndIsActive(ModalityType modalityType, Boolean isActive);

    Optional<ProcedureCatalog> findByNameAndIsActive(String name, Boolean isActive);

    @Query("SELECT p FROM ProcedureCatalog p WHERE " +
           "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:modalityCode IS NULL OR p.modalityType.code = :modalityCode) AND " +
           "(:region IS NULL OR p.region = :region) AND " +
           "p.isActive = true")
    List<ProcedureCatalog> searchProcedures(@Param("name") String name,
                                           @Param("modalityCode") String modalityCode,
                                           @Param("region") String region);

    @Query("SELECT DISTINCT p.modalityType.code FROM ProcedureCatalog p WHERE p.isActive = true ORDER BY p.modalityType.sortOrder ASC, p.modalityType.code ASC")
    List<String> findDistinctModalityCodes();

    @Query("SELECT DISTINCT p.region FROM ProcedureCatalog p WHERE p.isActive = true AND p.region IS NOT NULL ORDER BY p.region")
    List<String> findDistinctRegions();

    @Query("SELECT p FROM ProcedureCatalog p LEFT JOIN FETCH p.modalityType WHERE p.isActive = true")
    List<ProcedureCatalog> findAllWithModality();
}
