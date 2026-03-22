package com.application.service;

import com.application.entity.ExamStatusMessage;
import com.application.util.MppsEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service métier pour le traitement des événements MPPS.
 *
 * Implémentez ici votre logique RIS :
 *  - Mise à jour du statut d'un ordre de travail (worklist)
 *  - Enregistrement en base de données
 *  - Notification d'autres composants (WebSocket, événements Spring, etc.)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MppsService {

    private final ExamStatusWebSocketService webSocketService;
    
    // Injectez ici vos repositories / autres services RIS
    // private final WorklistRepository worklistRepository;
    // private final ApplicationEventPublisher eventPublisher;

    /**
     * Modality démarre l'acquisition.
     * Status DICOM : IN PROGRESS
     * MPPS DICOM : N-CREATE
     */
    public void handleNCreate(MppsEvent event) {
        log.info("[MPPS-SERVICE] N-CREATE → Acquisition started | " +
                        "Patient={} | Accession={} | Modality={} | AET={} | StartDate={} StartTime={}",
                event.getPatientId(),
                event.getAccessionNumber(),
                event.getModalityType(),
                event.getCallingAet(),
                event.getPerformedProcedureStepStartDate(),
                event.getPerformedProcedureStepStartTime());

        log.debug("[MPPS-DEBUG] Création message WebSocket pour N-CREATE");
        log.debug("[MPPS-DEBUG] AccessionNumber: {}", event.getAccessionNumber());
        log.debug("[MPPS-DEBUG] PatientName: {}", event.getPatientName());
        log.debug("[MPPS-DEBUG] ModalityType: {}", event.getModalityType());
        log.debug("[MPPS-DEBUG] CallingAET: {}", event.getCallingAet());

        // Notification WebSocket pour le début d'acquisition
        ExamStatusMessage message = new ExamStatusMessage(
                event.getAccessionNumber(),
                event.getPatientName(),
                event.getModalityType(),
                "SCHEDULED",
                "IN_PROGRESS",
                "Acquisition démarrée par " + event.getCallingAet()
        );
        
        log.debug("[MPPS-DEBUG] Message WebSocket créé: {}", message);
        log.debug("[MPPS-DEBUG] Envoi WebSocket sur /topic/exam-status");
        
        webSocketService.sendStatusUpdate(message);
        
        log.info("[MPPS-DEBUG] Notification WebSocket N-CREATE envoyée avec succès");

        // ── Exemple d'implémentation ───────────────────────────────────────
        // 1. Trouver la worklist correspondante via accessionNumber ou studyInstanceUID
        // Optional<Worklist> wl = worklistRepository.findByAccessionNumber(event.getAccessionNumber());
        //
        // 2. Mettre à jour le statut
        // wl.ifPresent(w -> {
        //     w.setStatus(WorklistStatus.IN_PROGRESS);
        //     w.setModalityAet(event.getCallingAet());
        //     w.setStartDate(parseDate(event.getPerformedProcedureStepStartDate(),
        //                             event.getPerformedProcedureStepStartTime()));
        //     worklistRepository.save(w);
        // });
        //
        // 3. Publier un événement Spring pour notifier d'autres services
        // eventPublisher.publishEvent(new MppsNCreateEvent(this, event));
        // ──────────────────────────────────────────────────────────────────
    }

    /**
     * Modality termine ou annule l'acquisition.
     * Status DICOM : COMPLETED | DISCONTINUED
     * MPPS DICOM : N-SET
     */
    public void handleNSet(MppsEvent event) {
        String status = event.getPerformedProcedureStepStatus();

        if ("COMPLETED".equalsIgnoreCase(status)) {
            log.info("[MPPS-SERVICE] N-SET COMPLETED → Acquisition done | " +
                            "Patient={} | Accession={} | EndDate={} EndTime={}",
                    event.getPatientId(),
                    event.getAccessionNumber(),
                    event.getPerformedProcedureStepEndDate(),
                    event.getPerformedProcedureStepEndTime());

            // Notification WebSocket pour la fin d'acquisition
            ExamStatusMessage message = new ExamStatusMessage(
                    event.getAccessionNumber(),
                    event.getPatientName(),
                    event.getModalityType(),
                    "IN_PROGRESS",
                    "COMPLETED",
                    "Acquisition terminée par " + event.getCallingAet()
            );
            webSocketService.sendStatusUpdate(message);

            // ── Exemple ───────────────────────────────────────────────────
            // wl.setStatus(WorklistStatus.COMPLETED);
            // wl.setEndDate(parseDate(event.getPerformedProcedureStepEndDate(),
            //                        event.getPerformedProcedureStepEndTime()));
            // worklistRepository.save(wl);
            // ─────────────────────────────────────────────────────────────

        } else if ("DISCONTINUED".equalsIgnoreCase(status)) {
            log.warn("[MPPS-SERVICE] N-SET DISCONTINUED → Acquisition cancelled | " +
                            "Patient={} | Accession={}",
                    event.getPatientId(),
                    event.getAccessionNumber());

            // Notification WebSocket pour l'annulation
            ExamStatusMessage message = new ExamStatusMessage(
                    event.getAccessionNumber(),
                    event.getPatientName(),
                    event.getModalityType(),
                    "IN_PROGRESS",
                    "CANCELLED",
                    "Acquisition annulée par " + event.getCallingAet()
            );
            webSocketService.sendStatusUpdate(message);

            // ── Exemple ───────────────────────────────────────────────────
            // wl.setStatus(WorklistStatus.CANCELLED);
            // worklistRepository.save(wl);
            // ─────────────────────────────────────────────────────────────
        }
    }

    /**
     * Événement MPPS avec un statut non standard (fallback).
     */
    public void handleGenericEvent(MppsEvent event) {
        log.warn("[MPPS-SERVICE] Unknown MPPS status='{}' | SOP={} | AET={}",
                event.getPerformedProcedureStepStatus(),
                event.getSopInstanceUID(),
                event.getCallingAet());
        
        // Notification WebSocket pour les statuts inconnus
        ExamStatusMessage message = new ExamStatusMessage(
                event.getAccessionNumber(),
                event.getPatientName(),
                event.getModalityType(),
                "UNKNOWN",
                event.getPerformedProcedureStepStatus(),
                "Événement MPPS inconnu de " + event.getCallingAet()
        );
        webSocketService.sendStatusUpdate(message);
        
        // Log ou alerte uniquement — ne pas lever d'exception
    }
}