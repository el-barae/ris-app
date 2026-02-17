package com.application.dicom;

import com.application.entity.Exam;
import com.application.entity.ExamStatus;
import com.application.entity.ExamStatusMessage;
import com.application.entity.ProcedureStep;
import com.application.repository.ExamRepository;
import com.application.repository.ProcedureStepRepository;
import com.application.service.ExamStatusWebSocketService;
import org.dcm4che3.data.Attributes;
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
//    private final ExamStatusNotificationService notificationService;
    private final ExamStatusWebSocketService webSocketService;

    public MppsScp(ExamRepository repo,
                   ProcedureStepRepository procedureStepRepo,
//                   ExamStatusNotificationService notificationService,
                   ExamStatusWebSocketService webSocketService) {
        this.examRepo = repo;
        this.procedureStepRepo = procedureStepRepo;
//        this.notificationService = notificationService;
        this.webSocketService = webSocketService;
    }

    @Override
    public void onDimseRQ(Association as, PresentationContext pc, Dimse dimse,
                          Attributes cmd, Attributes data) throws IOException {

        // Vérifier si c'est bien du MPPS (N-CREATE ou N-SET)
        if (dimse != Dimse.N_CREATE_RQ && dimse != Dimse.N_SET_RQ) {
            super.onDimseRQ(as, pc, dimse, cmd, data);
            return;
        }

        System.out.println(" [MPPS] Message reçu : " + dimse);

        // 1. Récupérer les données
        String accessionNumber = null;
        String scheduledProcedureStepId = null;
        String status = null;

        if (data != null) {
            accessionNumber = data.getString(Tag.AccessionNumber);
            status = data.getString(Tag.PerformedProcedureStepStatus);
            
            // Récupérer le ScheduledProcedureStepID depuis la séquence
            Attributes scheduledStepSequence = data.getNestedDataset(Tag.ScheduledStepAttributesSequence);
            if (scheduledStepSequence != null) {
                scheduledProcedureStepId = scheduledStepSequence.getString(Tag.ScheduledProcedureStepID);
            }
        }

        System.out.println("   -> Accession : " + accessionNumber);
        System.out.println("   -> SPS ID : " + scheduledProcedureStepId);
        System.out.println("   -> Nouveau Statut : " + status);

        if (accessionNumber != null && !accessionNumber.isEmpty()) {
            try {
                // 2. Trouver l'examen en base
                Exam exam = examRepo.findByAccessionNumberWithRelations(accessionNumber).orElse(null);

                if (exam != null) {
                    ExamStatus oldExamStatus = exam.getStatus();
                    ExamStatus newExamStatus = null;
                    String statusMessage = "";
                    boolean examStatusChanged = false;

                    // 3. Gérer le SPS individuel si on a l'ID
                    if (scheduledProcedureStepId != null && !scheduledProcedureStepId.isEmpty()) {
                        ProcedureStep procedureStep = procedureStepRepo.findByScheduledProcedureStepId(scheduledProcedureStepId);
                        
                        if (procedureStep != null) {
                            boolean stepChanged = false;
                            
                            if ("IN PROGRESS".equalsIgnoreCase(status)) {
                                // Le SPS est en cours - pas de changement de statut pour le moment
                                System.out.println("   SPS " + scheduledProcedureStepId + " en cours");
                            } else if ("COMPLETED".equalsIgnoreCase(status) && !Boolean.TRUE.equals(procedureStep.getIsCompleted())) {
                                // Marquer le SPS comme complété
                                procedureStep.markAsCompleted("MPPS COMPLETED received");
                                procedureStepRepo.save(procedureStep);
                                stepChanged = true;
                                System.out.println("   SPS " + scheduledProcedureStepId + " marqué comme complété");
                                
                                // Vérifier si TOUS les SPS requis sont complétés
                                if (exam.getProcedure() != null) {
                                    long requiredCount = procedureStepRepo.countRequiredStepsByProcedureId(exam.getProcedure().getId());
                                    long completedCount = procedureStepRepo.countCompletedStepsByProcedureId(exam.getProcedure().getId());
                                    
                                    System.out.println("   Progression: " + completedCount + "/" + requiredCount + " SPS requis complétés");
                                    
                                    if (completedCount >= requiredCount && oldExamStatus != ExamStatus.COMPLETED) {
                                        newExamStatus = ExamStatus.COMPLETED;
                                        statusMessage = "Tous les steps sont terminés - Examen complété";
                                        examStatusChanged = true;
                                        System.out.println("   TOUS les SPS sont complétés -> Examen marqué COMPLETED");
                                    } else if (oldExamStatus == ExamStatus.PLANNED) {
                                        newExamStatus = ExamStatus.IN_PROGRESS;
                                        statusMessage = "Examen en cours (certains steps complétés)";
                                        examStatusChanged = true;
                                        System.out.println("   Premier SPS complété -> Examen marqué IN_PROGRESS");
                                    }
                                }
                            } else if ("DISCONTINUED".equalsIgnoreCase(status)) {
                                // Gérer l'annulation du SPS
                                procedureStep.markAsIncomplete();
                                procedureStepRepo.save(procedureStep);
                                stepChanged = true;
                                System.out.println("   SPS " + scheduledProcedureStepId + " annulé");
                            }
                            
                            // Envoyer une notification pour le SPS si changé
                            if (stepChanged) {
                                // TODO: Optionnel - envoyer notification spécifique au SPS
                            }
                        } else {
                            System.err.println("   SPS introuvable pour ID: " + scheduledProcedureStepId);
                        }
                    } else {
                        // Ancienne logique si pas de SPS ID (compatibilité)
                        if ("IN PROGRESS".equalsIgnoreCase(status) && oldExamStatus != ExamStatus.IN_PROGRESS) {
                            newExamStatus = ExamStatus.IN_PROGRESS;
                            statusMessage = "L'examen est en cours d'acquisition";
                            examStatusChanged = true;
                        } else if ("COMPLETED".equalsIgnoreCase(status) && oldExamStatus != ExamStatus.COMPLETED) {
                            newExamStatus = ExamStatus.COMPLETED;
                            statusMessage = "L'examen est terminé";
                            examStatusChanged = true;
                        } else if ("DISCONTINUED".equalsIgnoreCase(status) && oldExamStatus != ExamStatus.CANCELLED) {
                            newExamStatus = ExamStatus.CANCELLED;
                            statusMessage = "L'examen a été annulé";
                            examStatusChanged = true;
                        }
                    }

                    // 4. ENVOYER LE MESSAGE WEBSOCKET si le statut de l'examen a changé
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
                        System.out.println("   Message WebSocket envoyé");

                        // 5. Mettre à jour le statut de l'examen dans la base
                        exam.setStatus(newExamStatus);
                        examRepo.save(exam);
                        System.out.println("   Examen sauvegardé avec le statut: " + newExamStatus);
                    }
                } else {
                    System.err.println("   Examen introuvable pour Accession: " + accessionNumber);
                }
            } catch (Exception e) {
                System.err.println("   Erreur lors du traitement MPPS : " + e.getMessage());
                e.printStackTrace();
            }
        }

        // 7. Répondre "Succès" au robot
        Attributes responseCmd;
        if (dimse == Dimse.N_CREATE_RQ) {
            responseCmd = Commands.mkNCreateRSP(cmd, Status.Success);
        } else {
            responseCmd = Commands.mkNSetRSP(cmd, Status.Success);
        }

        as.writeDimseRSP(pc, responseCmd, null);
    }
}