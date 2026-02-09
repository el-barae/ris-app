package com.application.repository;

import com.application.entity.ScheduleSlot;
import com.application.entity.ScheduleSlotStatus;
import com.application.entity.Modality;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlot, Long> {

    @Query("SELECT s FROM ScheduleSlot s JOIN FETCH s.orderLine JOIN FETCH s.orderLine.patient JOIN FETCH s.orderLine.medecin JOIN FETCH s.modalityResource JOIN FETCH s.modalityResource.modalityType JOIN FETCH s.modalityResource.room WHERE s.modalityResource = :modalityResource AND s.status = :status ORDER BY s.scheduledStartTime")
    List<ScheduleSlot> findByModalityResourceAndStatusOrderByScheduledStartTime(
            Modality modalityResource, ScheduleSlotStatus status);

    List<ScheduleSlot> findByModalityResourceAndScheduledStartTimeBetweenOrderByScheduledStartTime(
            Modality modalityResource, LocalDateTime start, LocalDateTime end);

    @Query("SELECT s FROM ScheduleSlot s JOIN FETCH s.orderLine JOIN FETCH s.orderLine.patient JOIN FETCH s.orderLine.medecin JOIN FETCH s.modalityResource JOIN FETCH s.modalityResource.modalityType JOIN FETCH s.modalityResource.room WHERE s.status = :status ORDER BY s.scheduledStartTime")
    List<ScheduleSlot> findByStatusOrderByScheduledStartTime(ScheduleSlotStatus status);

    @Query("SELECT s FROM ScheduleSlot s WHERE s.modalityResource = :modality " +
           "AND s.scheduledStartTime < :endTime AND s.scheduledEndTime > :startTime " +
           "AND s.status NOT IN ('CANCELLED', 'COMPLETED')")
    List<ScheduleSlot> findConflictingSlots(
            @Param("modality") Modality modality,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT s FROM ScheduleSlot s JOIN FETCH s.orderLine JOIN FETCH s.modalityResource " +
           "WHERE s.scheduledStartTime >= :start AND s.scheduledStartTime <= :end " +
           "ORDER BY s.scheduledStartTime")
    List<ScheduleSlot> findSlotsBetweenDatesWithRelations(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    boolean existsByModalityResourceAndScheduledStartTimeBetween(
            Modality modalityResource, LocalDateTime start, LocalDateTime end);
}
