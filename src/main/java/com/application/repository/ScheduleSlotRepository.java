package com.application.repository;

import com.application.entity.ScheduleSlot;
import com.application.entity.ScheduleSlotStatus;
import com.application.entity.Modality;
import com.application.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlot, Long> {

    @Query("SELECT s FROM ScheduleSlot s JOIN FETCH s.orderLine e JOIN FETCH e.order o JOIN FETCH o.patient JOIN FETCH o.doctor JOIN FETCH s.modalityResource JOIN FETCH s.modalityResource.modalityType JOIN FETCH s.modalityResource.room WHERE s.modalityResource = :modalityResource AND s.status = :status ORDER BY s.scheduledStartTime")
    List<ScheduleSlot> findByModalityResourceAndStatusOrderByScheduledStartTime(
            Modality modalityResource, ScheduleSlotStatus status);

    List<ScheduleSlot> findByModalityResourceAndScheduledStartTimeBetweenOrderByScheduledStartTime(
            Modality modalityResource, LocalDateTime start, LocalDateTime end);

    @Query("SELECT s FROM ScheduleSlot s JOIN FETCH s.orderLine e JOIN FETCH e.order o JOIN FETCH o.patient JOIN FETCH o.doctor JOIN FETCH s.modalityResource JOIN FETCH s.modalityResource.modalityType JOIN FETCH s.modalityResource.room WHERE s.status = :status ORDER BY s.scheduledStartTime")
    List<ScheduleSlot> findByStatusOrderByScheduledStartTime(ScheduleSlotStatus status);

    @Query("SELECT s FROM ScheduleSlot s JOIN FETCH s.orderLine e JOIN FETCH e.order o JOIN FETCH o.patient JOIN FETCH o.doctor JOIN FETCH s.modalityResource JOIN FETCH s.modalityResource.modalityType JOIN FETCH s.modalityResource.room WHERE s.status = :status AND o.hospital.id = :hospitalId ORDER BY s.scheduledStartTime")
    List<ScheduleSlot> findByStatusAndHospitalIdOrderByScheduledStartTime(@Param("status") ScheduleSlotStatus status, @Param("hospitalId") Long hospitalId);

    @Query("SELECT s FROM ScheduleSlot s WHERE s.modalityResource = :modality " +
           "AND s.scheduledStartTime < :endTime AND s.scheduledEndTime > :startTime " +
           "AND s.status NOT IN ('CANCELLED', 'COMPLETED')")
    List<ScheduleSlot> findConflictingSlots(
            @Param("modality") Modality modality,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT s FROM ScheduleSlot s JOIN FETCH s.orderLine e JOIN FETCH e.order o JOIN FETCH s.modalityResource " +
           "WHERE s.scheduledStartTime >= :start AND s.scheduledStartTime <= :end " +
           "ORDER BY s.scheduledStartTime")
    List<ScheduleSlot> findSlotsBetweenDatesWithRelations(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    boolean existsByModalityResourceAndScheduledStartTimeBetween(
            Modality modalityResource, LocalDateTime start, LocalDateTime end);

    @Query("SELECT s FROM ScheduleSlot s LEFT JOIN FETCH s.orderLine e LEFT JOIN FETCH e.order o LEFT JOIN FETCH o.patient LEFT JOIN FETCH o.doctor LEFT JOIN FETCH s.modalityResource LEFT JOIN FETCH s.modalityResource.modalityType LEFT JOIN FETCH s.modalityResource.room LEFT JOIN FETCH s.technician WHERE s.technician.id = :technicianId AND s.status = :status ORDER BY s.scheduledStartTime")
    List<ScheduleSlot> findByTechnicianIdAndStatusOrderByScheduledStartTime(
            @Param("technicianId") Long technicianId, 
            @Param("status") ScheduleSlotStatus status);

    // Méthode simple pour tester
    List<ScheduleSlot> findByTechnicianIdAndStatus(Long technicianId, ScheduleSlotStatus status);

    List<ScheduleSlot> findByOrderLine(Exam orderLine);
}
