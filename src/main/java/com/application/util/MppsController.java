package com.application.util;

import com.application.service.MppsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * REST controller exposé à Orthanc pour recevoir les événements MPPS.
 *
 * Endpoints :
 *   POST /api/mpps/ncreate  → Modality démarre l'acquisition  (IN PROGRESS)
 *   POST /api/mpps/nset     → Modality termine l'acquisition  (COMPLETED / DISCONTINUED)
 *   POST /api/mpps/event    → Statuts inconnus / fallback
 *   GET  /api/mpps/health   → Sanity check
 */
@Slf4j
@RestController
@RequestMapping("/api/mpps")
@RequiredArgsConstructor
public class MppsController {

    private final MppsService mppsService;

    // ─────────────────────────────────────────────────────────────────────────
    // N-CREATE  →  début d'acquisition (IN PROGRESS)
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/ncreate")
    public ResponseEntity<Map<String, Object>> handleNCreate(
            @RequestBody MppsEvent event) {

        log.info("[MPPS] N-CREATE received | AET={} | Patient={} | Accession={} | Modality={}",
                event.getCallingAet(),
                event.getPatientId(),
                event.getAccessionNumber(),
                event.getModalityType());

        mppsService.handleNCreate(event);

        return ResponseEntity.ok(successResponse("N-CREATE processed", event));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // N-SET  →  fin / annulation d'acquisition (COMPLETED | DISCONTINUED)
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/nset")
    public ResponseEntity<Map<String, Object>> handleNSet(
            @RequestBody MppsEvent event) {

        log.info("[MPPS] N-SET received | AET={} | Patient={} | Accession={} | Status={}",
                event.getCallingAet(),
                event.getPatientId(),
                event.getAccessionNumber(),
                event.getPerformedProcedureStepStatus());

        mppsService.handleNSet(event);

        return ResponseEntity.ok(successResponse("N-SET processed", event));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Fallback pour les statuts non reconnus
    // ─────────────────────────────────────────────────────────────────────────
    @PostMapping("/event")
    public ResponseEntity<Map<String, Object>> handleGenericEvent(
            @RequestBody MppsEvent event) {

        log.warn("[MPPS] Generic MPPS event | Status={} | AET={} | SOP={}",
                event.getPerformedProcedureStepStatus(),
                event.getCallingAet(),
                event.getSopInstanceUID());

        mppsService.handleGenericEvent(event);

        return ResponseEntity.ok(successResponse("Generic event processed", event));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Health check — Orthanc peut l'appeler au démarrage pour vérifier la conn
    // ─────────────────────────────────────────────────────────────────────────
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "RIS MPPS Receiver",
                "timestamp", Instant.now().toString()
        ));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────
    private Map<String, Object> successResponse(String message, MppsEvent event) {
        return Map.of(
                "status", "OK",
                "message", message,
                "sopInstanceUID", event.getSopInstanceUID() != null ? event.getSopInstanceUID() : "",
                "accessionNumber", event.getAccessionNumber() != null ? event.getAccessionNumber() : "",
                "timestamp", Instant.now().toString()
        );
    }
}