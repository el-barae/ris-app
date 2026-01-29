package com.application.repository;

import com.application.entity.Modality;
import com.application.entity.ModalityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModalityRepository extends JpaRepository<Modality, Long> {

    List<Modality> findByIsActive(Boolean isActive);

    List<Modality> findByModalityTypeAndIsActive(ModalityType modalityType, Boolean isActive);

    Optional<Modality> findByAetitle(String aetitle);

    Optional<Modality> findByAetitleAndIsActive(String aetitle, Boolean isActive);

    List<Modality> findByModalityTypeId(Long modalityTypeId);

    @Query("SELECT m FROM Modality m WHERE " +
           "(:nom IS NULL OR LOWER(m.nom) LIKE LOWER(CONCAT('%', :nom, '%'))) AND " +
           "(:aetitle IS NULL OR LOWER(m.aetitle) LIKE LOWER(CONCAT('%', :aetitle, '%'))) AND " +
           "(:marque IS NULL OR LOWER(m.marque) LIKE LOWER(CONCAT('%', :marque, '%'))) AND " +
           "(:modalityTypeId IS NULL OR m.modalityType.id = :modalityTypeId) AND " +
           "m.isActive = true")
    List<Modality> searchModalities(@Param("nom") String nom, 
                                   @Param("aetitle") String aetitle, 
                                   @Param("marque") String marque, 
                                   @Param("modalityTypeId") Long modalityTypeId);

    @Query("SELECT m FROM Modality m WHERE m.isActive = true ORDER BY m.nom ASC")
    List<Modality> findAllActiveOrdered();

    boolean existsByAetitle(String aetitle);
}
