package com.application.util;

import com.application.service.OrthancNotificationService;
import com.application.service.OrthancStudyArrivedRequest;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/orthanc")
@Slf4j
public class OrthancWebhookController {

    @Autowired
    private OrthancNotificationService orthancNotificationService;

    public OrthancWebhookController() {
        log.info("🚀 OrthancWebhookController initialisé !");
    }

    /**
     * Endpoint appelé par le script Lua/Python d'Orthanc Central
     * quand une étude est stable et reçue
     */
    @PostMapping("/study-arrived")
    public ResponseEntity<Map<String, String>> onStudyArrived(
            @RequestBody OrthancStudyArrivedRequest request) {

        log.info("🏥 Webhook Orthanc reçu - Accession: {} | Patient: {}",
                request.getAccessionNumber(), request.getPatientName());

        try {
            orthancNotificationService.handleOrthancStudyArrived(request);
            return ResponseEntity.ok(Map.of(
                    "status", "OK",
                    "message", "ProcedureStep complété et interface notifiée",
                    "accessionNumber", request.getAccessionNumber()
            ));
        } catch (EntityNotFoundException e) {
            log.warn("⚠️ ProcedureStep non trouvé: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "NOT_FOUND", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ Erreur traitement webhook Orthanc: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "ERROR", "message", e.getMessage()));
        }
    }
}