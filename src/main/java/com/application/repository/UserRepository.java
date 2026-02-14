package com.application.repository;

import com.application.entity.User;
import com.application.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    List<User> findByRole(UserRole role);

    List<User> findByActiveTrue();
    
    // Charger l'utilisateur avec son hôpital pour éviter les problèmes de lazy loading
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.hospital WHERE u.username = :username")
    Optional<User> findByUsernameWithHospital(String username);
}
