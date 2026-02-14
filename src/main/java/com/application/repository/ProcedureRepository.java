package com.application.repository;

import com.application.entity.Procedure;
import com.application.entity.ProcedureCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcedureRepository extends JpaRepository<Procedure, Long> {

    List<Procedure> findByProcedureCatalog(ProcedureCatalog procedureCatalog);

    List<Procedure> findByProcedureCatalogId(Long procedureCatalogId);

    @Query("SELECT p FROM Procedure p WHERE " +
           "(:catalogId IS NULL OR p.procedureCatalog.id = :catalogId) AND " +
           "(:emergency IS NULL OR p.isEmergency = :emergency)")
    List<Procedure> searchProcedures(@Param("catalogId") Long catalogId, 
                                   @Param("emergency") Boolean emergency);

    @Query("SELECT p FROM Procedure p WHERE p.isEmergency = true ORDER BY p.createdAt DESC")
    List<Procedure> findEmergencyProcedures();

    @Query("SELECT p FROM Procedure p WHERE p.contrastRequired = true ORDER BY p.createdAt DESC")
    List<Procedure> findProceduresWithContrast();
}
