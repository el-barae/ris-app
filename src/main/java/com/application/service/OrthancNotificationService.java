package com.application.service;

import com.application.entity.ExamStatusMessage;
import com.application.entity.ProcedureStep;
import com.application.repository.ProcedureStepRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
@Transactional
public class OrthancNotificationService {

    @Autowired
    private ProcedureStepRepository procedureStepRepository;

    @Autowired
    private ExamStatusWebSocketService webSocketService;

    /**
     * Traite la notification d'Orthanc Central :
     *  1. Trouve le ProcedureStep via l'accessionNumber
     *  2. Notifie l'interface via WebSocket
     *  3. Marque le step en COMPLETED
     */
    public void handleOrthancStudyArrived(OrthancStudyArrivedRequest request) {
        String accessionNumber = request.getAccessionNumber();
        log.info("📥 Notification Orthanc reçue pour accession: {}", accessionNumber);

        // Recherche du ProcedureStep lié à cet accession number
        List<ProcedureStep> steps = procedureStepRepository.findPendingStepsByAccessionNumber(accessionNumber);
        if (steps.isEmpty()) {
            log.error("❌ Aucun ProcedureStep trouvé pour accession: {}", accessionNumber);
            throw new EntityNotFoundException(
                    "ProcedureStep introuvable pour accessionNumber: " + accessionNumber
            );
        }
        
        ProcedureStep step = steps.get(0); // Prendre le premier step en attente

        if (step.getIsCompleted()) {
            log.warn("⚠️ ProcedureStep déjà complété pour accession: {}", accessionNumber);
            // On notifie quand même l'interface
            sendWebSocketNotification(request, step, "Images déjà enregistrées dans le PACS");
            return;
        }

        // 1. Notification WebSocket AVANT la complétion (étude bien reçue dans PACS)
        sendWebSocketNotification(request, step, "Images reçues et enregistrées dans le PACS central");

        // 2. Complétion du ProcedureStep
        String completionNotes = String.format(
                "Étude reçue dans Orthanc Central le %s | StudyUID: %s | Modalité: %s",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                request.getStudyUID(),
                request.getModality()
        );
        step.markAsCompleted(completionNotes);
        procedureStepRepository.save(step);

        log.info("✅ ProcedureStep [{}] marqué COMPLETED pour accession: {}",
                step.getId(), accessionNumber);
    }

    private void sendWebSocketNotification(OrthancStudyArrivedRequest request,
                                           ProcedureStep step,
                                           String message) {
        ExamStatusMessage wsMessage = new ExamStatusMessage();
        wsMessage.setAccessionNumber(request.getAccessionNumber());
        wsMessage.setPatientName(request.getPatientName());
        wsMessage.setExamType(request.getModality() != null
                ? request.getModality()
                : step.getName());
        wsMessage.setNewStatus("COMPLETED");
        wsMessage.setMessage(message);

        webSocketService.sendStatusUpdate(wsMessage);
        log.info("📡 Notification WebSocket envoyée pour: {}", request.getAccessionNumber());
    }
}
