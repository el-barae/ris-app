package com.application.dicom;

import com.application.entity.Exam;
import com.application.entity.ExamStatus;
import com.application.entity.ExamStatusMessage;
import com.application.entity.ProcedureStep;
import com.application.repository.ExamRepository;
import com.application.repository.ProcedureStepRepository;
import com.application.service.ExamStatusWebSocketService;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Sequence;
import org.dcm4che3.data.Tag;
import org.dcm4che3.net.*;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicMPPSSCP;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.Commands;

import java.io.IOException;

public class MppsScp extends BasicMPPSSCP {

    private final ExamRepository examRepo;
    private final ProcedureStepRepository procedureStepRepo;
    private final ExamStatusWebSocketService webSocketService;

    public MppsScp(ExamRepository repo,
                   ProcedureStepRepository procedureStepRepo,
                   ExamStatusWebSocketService webSocketService) {
        this.examRepo = repo;
        this.procedureStepRepo = procedureStepRepo;
        this.webSocketService = webSocketService;
    }

    @Override
    public void onDimseRQ(Association as, PresentationContext pc, Dimse dimse,
                          Attributes cmd, Attributes data) throws IOException {

        if (dimse != Dimse.N_CREATE_RQ && dimse != Dimse.N_SET_RQ) {
            super.onDimseRQ(as, pc, dimse, cmd, data);
            return;
        }

        System.out.println("[MPPS] Message reçu : " + dimse);

        String accessionNumber        = null;
        String scheduledProcedureStepId = null;
        String status                 = null;

        if (data != null) {
            accessionNumber = data.getString(Tag.AccessionNumber);
            status          = data.getString(Tag.PerformedProcedureStepStatus);

            // Lire le SPS ID depuis ScheduledStepAttributesSequence
            Sequence scheduledStepsSeq = data.getSequence(Tag.ScheduledStepAttributesSequence);
            if (scheduledStepsSeq != null && !scheduledStepsSeq.isEmpty()) {
                Attributes firstStep = scheduledStepsSeq.get(0);
                if (firstStep != null) {
                    scheduledProcedureStepId = firstStep.getString(Tag.ScheduledProcedureStepID);
                }
            }
            // Fallback direct
            if (scheduledProcedureStepId == null) {
                scheduledProcedureStepId = data.getString(Tag.ScheduledProcedureStepID);
            }
        }

        System.out.println("   -> Accession     : " + accessionNumber);
        System.out.println("   -> SPS ID reçu   : " + scheduledProcedureStepId);
        System.out.println("   -> Statut        : " + status);

        if (accessionNumber != null && !accessionNumber.isEmpty()) {
            try {
                Exam exam = examRepo.findByAccessionNumberWithRelations(accessionNumber).orElse(null);

                if (exam != null) {
                    ExamStatus oldExamStatus   = exam.getStatus();
                    ExamStatus newExamStatus   = null;
                    String     statusMessage   = "";
                    boolean    examStatusChanged = false;

                    // ── Résolution du ProcedureStep ──────────────────────────
                    ProcedureStep procedureStep = resolveProcedureStep(
                            scheduledProcedureStepId, accessionNumber, exam);

                    if (procedureStep == null) {
                        System.err.println("   -> Aucun ProcedureStep trouvé — fallback logique examen global");
                    }

                    // ── Mise à jour statut ────────────────────────────────────
                    if (procedureStep != null) {
                        // Mise à jour du step individuel
                        if ("IN PROGRESS".equalsIgnoreCase(status)) {
                            System.out.println("   -> Step marqué IN PROGRESS : " + procedureStep.getScheduledProcedureStepId());
                            // Pas de markAsInProgress sur le step, juste l'examen
                        } else if ("COMPLETED".equalsIgnoreCase(status) && !Boolean.TRUE.equals(procedureStep.getIsCompleted())) {
                            procedureStep.markAsCompleted("MPPS COMPLETED received");
                            procedureStepRepo.save(procedureStep);
                            System.out.println("   -> Step complété : " + procedureStep.getScheduledProcedureStepId());
                        } else if ("DISCONTINUED".equalsIgnoreCase(status)) {
                            procedureStep.markAsIncomplete();
                            procedureStepRepo.save(procedureStep);
                            System.out.println("   -> Step annulé : " + procedureStep.getScheduledProcedureStepId());
                        }

                        // Vérifier la progression globale
                        if (exam.getProcedure() != null) {
                            long required  = procedureStepRepo.countRequiredStepsByProcedureId(exam.getProcedure().getId());
                            long completed = procedureStepRepo.countCompletedStepsByProcedureId(exam.getProcedure().getId());
                            System.out.println("   -> Progression : " + completed + "/" + required + " SPS");

                            if ("COMPLETED".equalsIgnoreCase(status) && completed >= required
                                    && oldExamStatus != ExamStatus.COMPLETED) {
                                newExamStatus   = ExamStatus.COMPLETED;
                                statusMessage   = "Tous les steps sont terminés";
                                examStatusChanged = true;
                            } else if ("IN PROGRESS".equalsIgnoreCase(status)
                                    && oldExamStatus == ExamStatus.PLANNED) {
                                newExamStatus   = ExamStatus.IN_PROGRESS;
                                statusMessage   = "Examen en cours d'acquisition";
                                examStatusChanged = true;
                            } else if ("DISCONTINUED".equalsIgnoreCase(status)
                                    && oldExamStatus != ExamStatus.CANCELLED) {
                                newExamStatus   = ExamStatus.CANCELLED;
                                statusMessage   = "Examen annulé";
                                examStatusChanged = true;
                            }
                        }

                    } else {
                        // Fallback : pas de step trouvé → mettre à jour l'examen directement
                        if ("IN PROGRESS".equalsIgnoreCase(status) && oldExamStatus != ExamStatus.IN_PROGRESS) {
                            newExamStatus   = ExamStatus.IN_PROGRESS;
                            statusMessage   = "Examen en cours (aucun step trouvé)";
                            examStatusChanged = true;
                        } else if ("COMPLETED".equalsIgnoreCase(status) && oldExamStatus != ExamStatus.COMPLETED) {
                            newExamStatus   = ExamStatus.COMPLETED;
                            statusMessage   = "Examen terminé (aucun step trouvé)";
                            examStatusChanged = true;
                        } else if ("DISCONTINUED".equalsIgnoreCase(status) && oldExamStatus != ExamStatus.CANCELLED) {
                            newExamStatus   = ExamStatus.CANCELLED;
                            statusMessage   = "Examen annulé (aucun step trouvé)";
                            examStatusChanged = true;
                        }
                    }

                    // ── WebSocket + Save ──────────────────────────────────────
                    if (examStatusChanged && newExamStatus != null) {
                        ExamStatusMessage wsMessage = new ExamStatusMessage(
                                exam.getAccessionNumber(),
                                exam.getPatient().getFirstName() + " " + exam.getPatient().getLastName(),
                                exam.getModalityCode() != null ? exam.getModalityCode() : "UNKNOWN",
                                oldExamStatus.toString(),
                                newExamStatus.toString(),
                                statusMessage
                        );
                        webSocketService.sendStatusUpdate(wsMessage);
                        exam.setStatus(newExamStatus);
                        examRepo.save(exam);
                        System.out.println("   -> Examen sauvegardé : " + newExamStatus);
                    }

                } else {
                    System.err.println("   -> Examen introuvable pour Accession : " + accessionNumber);
                }
            } catch (Exception e) {
                System.err.println("   -> Erreur traitement MPPS : " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Répondre Succès dans tous les cas
        Attributes responseCmd;
        if (dimse == Dimse.N_CREATE_RQ) {
            responseCmd = Commands.mkNCreateRSP(cmd, Status.Success);
        } else {
            responseCmd = Commands.mkNSetRSP(cmd, Status.Success);
        }
        as.writeDimseRSP(pc, responseCmd, null);
    }

    /**
     * Résout le ProcedureStep par SPS ID en essayant plusieurs stratégies.
     *
     * Stratégie 1 : correspondance exacte sur scheduledProcedureStepId
     * Stratégie 2 : le SPS ID contient l'accessionNumber → on cherche le premier
     *               step de l'examen (cas des IDs générés : "SPS-ACC202602141649287809")
     * Stratégie 3 : extraction numérique courte (ex: "SPS-CT-001" → 1 → step à l'index 0)
     * Stratégie 4 : premier step de la procédure (dernier recours)
     */
    private ProcedureStep resolveProcedureStep(String spsId, String accessionNumber, Exam exam) {

        // Stratégie 1 : ID exact
        if (spsId != null && !spsId.isEmpty()) {
            ProcedureStep step = procedureStepRepo.findByScheduledProcedureStepId(spsId);
            if (step != null) {
                System.out.println("   -> Step trouvé par ID exact : " + spsId);
                return step;
            }
        }

        // Stratégie 2 : SPS ID contient l'accessionNumber
        // Ex: "SPS-ACC202602141649287809" → l'examen n'a probablement qu'un seul step
        if (spsId != null && accessionNumber != null && spsId.contains(accessionNumber)) {
            System.out.println("   -> SPS ID contient l'AccessionNumber → recherche par accession");
            if (exam.getProcedure() != null) {
                java.util.List<ProcedureStep> steps =
                        procedureStepRepo.findByProcedureIdOrderByStepOrder(exam.getProcedure().getId());
                if (!steps.isEmpty()) {
                    System.out.println("   -> Premier step de la procédure utilisé : "
                            + steps.get(0).getScheduledProcedureStepId());
                    return steps.get(0);
                }
            }
        }

        // Stratégie 3 : extraire un petit numéro de l'ID
        // Ex: "SPS-CT-001" → 1 → index 0, "STEP-002" → 2 → index 1
        if (spsId != null && exam.getProcedure() != null) {
            String shortNum = extractShortNumericId(spsId);
            if (shortNum != null) {
                try {
                    int position = Integer.parseInt(shortNum);
                    java.util.List<ProcedureStep> steps =
                            procedureStepRepo.findByProcedureIdOrderByStepOrder(exam.getProcedure().getId());
                    if (position >= 1 && position <= steps.size()) {
                        ProcedureStep step = steps.get(position - 1);
                        System.out.println("   -> Step trouvé par position " + position + " : "
                                + step.getScheduledProcedureStepId());
                        return step;
                    }
                } catch (NumberFormatException ignored) {
                    // shortNum dépasse int → passer à la stratégie suivante
                }
            }
        }

        // Stratégie 4 : premier step disponible (dernier recours)
        if (exam.getProcedure() != null) {
            java.util.List<ProcedureStep> steps =
                    procedureStepRepo.findByProcedureIdOrderByStepOrder(exam.getProcedure().getId());
            if (!steps.isEmpty()) {
                System.out.println("   -> Dernier recours : premier step utilisé : "
                        + steps.get(0).getScheduledProcedureStepId());
                return steps.get(0);
            }
        }

        return null;
    }

    /**
     * Extrait un identifiant numérique COURT (≤ 9 chiffres, tient dans un int).
     * Ignore les grands nombres qui sont en fait des timestamps ou accessionNumbers.
     * Ex: "SPS-CT-001"               → "1"
     *     "STEP-002"                  → "2"
     *     "SPS-ACC202602141649287809" → null  (trop grand → stratégie 2 préférable)
     */
    private String extractShortNumericId(String complexId) {
        if (complexId == null || complexId.isEmpty()) return null;

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\d+");
        java.util.regex.Matcher matcher = pattern.matcher(complexId);

        while (matcher.find()) {
            String numericPart = matcher.group();
            // Ignorer les grands nombres (timestamps, accessions, UIDs partiels)
            if (numericPart.length() <= 9) {
                return numericPart.replaceFirst("^0+(?!$)", ""); // retirer zéros non significatifs
            }
        }
        return null;
    }
}