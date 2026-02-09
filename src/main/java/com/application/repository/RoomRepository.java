package com.application.repository;

import com.application.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByNameContainingIgnoreCase(String name);

    @Query("SELECT r FROM Room r JOIN FETCH r.modalityResources WHERE r.id = :id")
    Room findByIdWithModalityResources(Long id);

    @Query("SELECT r FROM Room r JOIN FETCH r.modalityResources ORDER BY r.name")
    List<Room> findAllWithModalityResources();
}
