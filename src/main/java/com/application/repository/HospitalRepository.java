package com.application.repository;

import com.application.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {
    
    Optional<Hospital> findByName(String name);
    
    List<Hospital> findByIsActive(Boolean isActive);
    
    List<Hospital> findByCity(String city);
    
    List<Hospital> findByCountry(String country);
    
    boolean existsByName(String name);
}
