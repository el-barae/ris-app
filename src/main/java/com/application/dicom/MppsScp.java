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

        String accessionNumber = null;
        String receivedSpsId   = null;
        String mppsStatus      = null;

        if (data != null) {
            accessionNumber = data.getString(Tag.AccessionNumber);
            mppsStatus      = data.getString(Tag.PerformedProcedureStepStatus);

            Sequence seq = data.getSequence(Tag.ScheduledStepAttributesSequence);
            if (seq != null && !seq.isEmpty()) {
                Attributes first = seq.get(0);
                if (first != null) {
                    receivedSpsId = first.getString(Tag.ScheduledProcedureStepID);
                }
            }
            if (receivedSpsId == null) {
                receivedSpsId = data.getString(Tag.ScheduledProcedureStepID);
            }
        }

        System.out.println("   -> Accession : " + accessionNumber);
        System.out.println("   -> SPS ID    : " + receivedSpsId);
        System.out.println("   -> Statut    : " + mppsStatus);

        if (accessionNumber == null || accessionNumber.isEmpty()) {
            replySuccess(as, pc, dimse, cmd);
            return;
        }

        try {
            Exam exam = examRepo.findByAccessionNumberWithRelations(accessionNumber).orElse(null);
            if (exam == null) {
                System.err.println("   -> Examen introuvable : " + accessionNumber);
                replySuccess(as, pc, dimse, cmd);
                return;
            }

            ExamStatus oldExamStatus = exam.getStatus();

            // Résolution du step
            ProcedureStep procedureStep = resolveProcedureStep(receivedSpsId, accessionNumber, exam);

            // ── Si on a trouvé un step et que son scheduled_procedure_step_id
            //    est NULL, on le renseigne maintenant pour les prochains appels ──
            if (procedureStep != null
                    && (procedureStep.getScheduledProcedureStepId() == null
                    || procedureStep.getScheduledProcedureStepId().isEmpty())
                    && receivedSpsId != null) {
                procedureStep.setScheduledProcedureStepId(receivedSpsId);
                procedureStepRepo.save(procedureStep);
                System.out.println("   -> scheduled_procedure_step_id renseigné : " + receivedSpsId);
            }

            // Label d'affichage pour le step
            String displaySpsId = (procedureStep != null && procedureStep.getScheduledProcedureStepId() != null)
                    ? procedureStep.getScheduledProcedureStepId()
                    : (receivedSpsId != null ? receivedSpsId : "Step-" + (procedureStep != null ? procedureStep.getId() : "?"));

            if (procedureStep != null) {
                handleStepUpdate(exam, procedureStep, displaySpsId, mppsStatus, oldExamStatus);
            } else {
                handleExamFallback(exam, mppsStatus, oldExamStatus, receivedSpsId);
            }

        } catch (Exception e) {
            System.err.println("   -> Erreur traitement MPPS : " + e.getMessage());
            e.printStackTrace();
        }

        replySuccess(as, pc, dimse, cmd);
    }

    // =========================================================================
    //  Logique de mise à jour
    // =========================================================================

    private void handleStepUpdate(Exam exam, ProcedureStep procedureStep, String displaySpsId,
                                  String mppsStatus, ExamStatus oldExamStatus) {

        String patientName = exam.getPatient().getFirstName() + " " + exam.getPatient().getLastName();
        String modality    = exam.getModalityCode() != null ? exam.getModalityCode() : "UNKNOWN";

        if ("IN PROGRESS".equalsIgnoreCase(mppsStatus)) {

            System.out.println("   -> Step IN PROGRESS : " + displaySpsId);

            // Toujours mettre à jour le statut de l'examen à IN_PROGRESS
            ExamStatus newExamStatus = ExamStatus.IN_PROGRESS;
            if (oldExamStatus != ExamStatus.IN_PROGRESS) {
                exam.setStatus(newExamStatus);
                examRepo.save(exam);
                System.out.println("   -> Examen -> IN_PROGRESS (était " + oldExamStatus + ")");
            }

            // Envoyer notification "step en cours"
            webSocketService.sendStatusUpdate(new ExamStatusMessage(
                    exam.getAccessionNumber(), patientName, modality,
                    oldExamStatus.toString(), newExamStatus.toString(),
                    "Step en cours d'acquisition : " + displaySpsId
            ));

        } else if ("COMPLETED".equalsIgnoreCase(mppsStatus)
                && !Boolean.TRUE.equals(procedureStep.getIsCompleted())) {

            // Marquer le step comme complété
            procedureStep.markAsCompleted("MPPS COMPLETED received");
            procedureStepRepo.save(procedureStep);
            System.out.println("   -> Step COMPLETED : " + displaySpsId);

            // Envoyer notification "step complété"
            webSocketService.sendStatusUpdate(new ExamStatusMessage(
                    exam.getAccessionNumber(), patientName, modality,
                    exam.getStatus().toString(), exam.getStatus().toString(),
                    "Step complété : " + displaySpsId
            ));

            // Vérifier si tous les steps sont complétés pour mettre l'examen à COMPLETED
            long totalSteps = 0;
            long completedSteps = 0;
            if (exam.getProcedure() != null) {
                Long procedureId = exam.getProcedure().getId();
                // Compter les steps ayant un SPS ID assigné (participent au MPPS)
                totalSteps = procedureStepRepo.countByProcedureIdAndScheduledProcedureStepIdIsNotNull(procedureId);
                completedSteps = procedureStepRepo.countByProcedureIdAndIsCompletedTrue(procedureId);

                // Fallback : si aucun step n'a de SPS ID, compter tous les steps
                if (totalSteps == 0) {
                    totalSteps = procedureStepRepo.countByProcedureId(procedureId);
                    completedSteps = procedureStepRepo.countByProcedureIdAndIsCompletedTrue(procedureId);
                }
            }
            if (totalSteps == 0) totalSteps = 1;
            System.out.println("   -> Progression : " + completedSteps + "/" + totalSteps + " steps");

            boolean allDone = completedSteps >= totalSteps;
            if (allDone && exam.getStatus() != ExamStatus.COMPLETED) {
                exam.setStatus(ExamStatus.COMPLETED);
                examRepo.save(exam);
                System.out.println("   -> Examen -> COMPLETED (tous les steps terminés)");

                // Envoyer notification examen complété
                webSocketService.sendStatusUpdate(new ExamStatusMessage(
                        exam.getAccessionNumber(), patientName, modality,
                        ExamStatus.IN_PROGRESS.toString(), ExamStatus.COMPLETED.toString(),
                        "Examen terminé : tous les steps sont complétés (" + completedSteps + "/" + totalSteps + ")"
                ));
            }

        } else if ("DISCONTINUED".equalsIgnoreCase(mppsStatus)) {

            procedureStep.markAsIncomplete();
            procedureStepRepo.save(procedureStep);
            System.out.println("   -> Step DISCONTINUED : " + displaySpsId);

            if (exam.getStatus() != ExamStatus.CANCELLED) {
                exam.setStatus(ExamStatus.CANCELLED);
                examRepo.save(exam);
            }
            webSocketService.sendStatusUpdate(new ExamStatusMessage(
                    exam.getAccessionNumber(), patientName, modality,
                    exam.getStatus().toString(), ExamStatus.CANCELLED.toString(),
                    "Step annulé : " + displaySpsId
            ));
        }
    }

    private void handleExamFallback(Exam exam, String mppsStatus,
                                    ExamStatus oldExamStatus, String spsId) {
        String patientName = exam.getPatient().getFirstName() + " " + exam.getPatient().getLastName();
        String modality    = exam.getModalityCode() != null ? exam.getModalityCode() : "UNKNOWN";
        String label       = spsId != null ? spsId : "?";

        ExamStatus newStatus = null;
        String message = "";

        if ("IN PROGRESS".equalsIgnoreCase(mppsStatus) && isBeforeInProgress(oldExamStatus)) {
            newStatus = ExamStatus.IN_PROGRESS;
            message   = "Step en cours : " + label;
        } else if ("COMPLETED".equalsIgnoreCase(mppsStatus) && oldExamStatus != ExamStatus.COMPLETED) {
            newStatus = ExamStatus.COMPLETED;
            message   = "Examen terminé — step : " + label;
        } else if ("DISCONTINUED".equalsIgnoreCase(mppsStatus) && oldExamStatus != ExamStatus.CANCELLED) {
            newStatus = ExamStatus.CANCELLED;
            message   = "Examen annulé — step : " + label;
        }

        if (newStatus != null) {
            exam.setStatus(newStatus);
            examRepo.save(exam);
            webSocketService.sendStatusUpdate(new ExamStatusMessage(
                    exam.getAccessionNumber(), patientName, modality,
                    oldExamStatus.toString(), newStatus.toString(), message));
        }
    }

    /** Retourne true si l'examen n'a pas encore commencé (avant IN_PROGRESS). */
    private boolean isBeforeInProgress(ExamStatus status) {
        String s = status.toString().toUpperCase();
        return s.equals("PLANNED") || s.equals("SELECTED") || s.equals("SCHEDULED");
    }

    // =========================================================================
    //  Résolution du ProcedureStep — 4 stratégies
    // =========================================================================

    private ProcedureStep resolveProcedureStep(String spsId, String accessionNumber, Exam exam) {

        if (exam.getProcedure() == null) return null;
        Long procedureId = exam.getProcedure().getId();

        // Stratégie 1 : correspondance exacte sur scheduled_procedure_step_id
        if (spsId != null && !spsId.isEmpty()) {
            ProcedureStep step = procedureStepRepo.findByScheduledProcedureStepId(spsId);
            if (step != null) {
                System.out.println("   -> Step trouvé par ID exact : " + spsId);
                return step;
            }
        }

        // Stratégie 2 : SPS ID contient l'accessionNumber (IDs type "SPS-ACC202602...")
        if (spsId != null && accessionNumber != null && spsId.contains(accessionNumber)) {
            System.out.println("   -> SPS ID contient AccessionNumber -> premier step non complété");
            java.util.List<ProcedureStep> steps =
                    procedureStepRepo.findByProcedureIdOrderByStepOrder(procedureId);
            for (ProcedureStep s : steps) {
                if (!Boolean.TRUE.equals(s.getIsCompleted())) {
                    System.out.println("   -> Step non complété (id=" + s.getId() + ")");
                    return s;
                }
            }
            if (!steps.isEmpty()) return steps.get(steps.size() - 1);
        }

        // Stratégie 3 : petit numéro dans l'ID ("SPS-CT-001" -> position 1)
        if (spsId != null) {
            String shortNum = extractShortNumericId(spsId);
            if (shortNum != null) {
                try {
                    int pos = Integer.parseInt(shortNum);
                    java.util.List<ProcedureStep> steps =
                            procedureStepRepo.findByProcedureIdOrderByStepOrder(procedureId);
                    if (pos >= 1 && pos <= steps.size()) {
                        System.out.println("   -> Step par position " + pos
                                + " (id=" + steps.get(pos - 1).getId() + ")");
                        return steps.get(pos - 1);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        // Stratégie 4 : premier step non complété (dernier recours)
        java.util.List<ProcedureStep> steps =
                procedureStepRepo.findByProcedureIdOrderByStepOrder(procedureId);
        for (ProcedureStep s : steps) {
            if (!Boolean.TRUE.equals(s.getIsCompleted())) {
                System.out.println("   -> Dernier recours (id=" + s.getId() + ")");
                return s;
            }
        }

        return null;
    }

    private String extractShortNumericId(String complexId) {
        if (complexId == null || complexId.isEmpty()) return null;
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile("\\d+").matcher(complexId);
        while (m.find()) {
            String num = m.group();
            if (num.length() <= 9) return num.replaceFirst("^0+(?!$)", "");
        }
        return null;
    }

    private void replySuccess(Association as, PresentationContext pc,
                              Dimse dimse, Attributes cmd) throws IOException {
        Attributes rsp = dimse == Dimse.N_CREATE_RQ
                ? Commands.mkNCreateRSP(cmd, Status.Success)
                : Commands.mkNSetRSP(cmd, Status.Success);
        as.writeDimseRSP(pc, rsp, null);
    }
}