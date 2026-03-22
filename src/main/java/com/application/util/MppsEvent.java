package com.application.util;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * DTO représentant un événement MPPS (Modality Performed Procedure Step)
 * envoyé par Orthanc via HTTP POST.
 *
 * Correspond aux champs construits dans mpps-handler.lua
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MppsEvent {

    // ── Identifiants DICOM ─────────────────────────────────────────────
    private String sopInstanceUID;
    private String sopClassUID;

    /** AET (Application Entity Title) de la modalité source */
    private String callingAet;

    // ── Patient ────────────────────────────────────────────────────────
    private String patientId;
    private String patientName;
    private String patientBirthDate;
    private String patientSex;

    // ── Étude / Procédure ──────────────────────────────────────────────
    private String studyInstanceUID;
    private String accessionNumber;
    private String modalityType;
    private String studyID;

    // ── Statut MPPS ────────────────────────────────────────────────────
    /**
     * Valeurs DICOM standard :
     *  - "IN PROGRESS"   → N-CREATE (début d'acquisition)
     *  - "COMPLETED"     → N-SET    (fin d'acquisition)
     *  - "DISCONTINUED"  → N-SET    (annulé)
     */
    private String performedProcedureStepStatus;

    // ── Dates / Heures ─────────────────────────────────────────────────
    private String performedProcedureStepStartDate;
    private String performedProcedureStepStartTime;
    private String performedProcedureStepEndDate;
    private String performedProcedureStepEndTime;

    // ── IDs procédure ──────────────────────────────────────────────────
    private String performedProcedureStepID;
    private String scheduledProcedureStepID;
    private String requestedProcedureID;

    // ── Référence Orthanc ──────────────────────────────────────────────
    private String orthancInstanceId;
}