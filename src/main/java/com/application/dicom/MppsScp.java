package com.application.dicom;

import com.application.entity.Exam;
import com.application.entity.ExamStatus;
import com.application.entity.ExamStatusMessage;
import com.application.repository.ExamRepository;
import com.application.service.ExamStatusNotificationService;
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
    private final ExamStatusNotificationService notificationService;
    private final ExamStatusWebSocketService webSocketService;

    public MppsScp(ExamRepository repo,
                   ExamStatusNotificationService notificationService,
                   ExamStatusWebSocketService webSocketService) {
        this.examRepo = repo;
        this.notificationService = notificationService;
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

        System.out.println("🔔 [MPPS] Message reçu : " + dimse);

        // 1. Récupérer les données
        String accessionNumber = null;
        String status = null;

        if (data != null) {
            accessionNumber = data.getString(Tag.AccessionNumber);
            status = data.getString(Tag.PerformedProcedureStepStatus);
        }

        System.out.println("   -> Accession : " + accessionNumber);
        System.out.println("   -> Nouveau Statut : " + status);

        if (accessionNumber != null && !accessionNumber.isEmpty()) {
            try {
                // 2. Trouver l'examen en base
                Exam exam = examRepo.findByAccessionNumberWithRelations(accessionNumber).orElse(null);

                if (exam != null) {
                    ExamStatus oldStatus = exam.getStatus();
                    ExamStatus newStatus = null;
                    String statusMessage = "";

                    // 3. Déterminer le nouveau statut
                    if ("IN PROGRESS".equalsIgnoreCase(status) && oldStatus != ExamStatus.IN_PROGRESS) {
                        newStatus = ExamStatus.IN_PROGRESS;
                        statusMessage = "L'examen est en cours d'acquisition";
                    } else if ("COMPLETED".equalsIgnoreCase(status) && oldStatus != ExamStatus.COMPLETED) {
                        newStatus = ExamStatus.COMPLETED;
                        statusMessage = "L'examen est terminé";
                    } else if ("DISCONTINUED".equalsIgnoreCase(status) && oldStatus != ExamStatus.CANCELLED) {
                        newStatus = ExamStatus.CANCELLED;
                        statusMessage = "L'examen a été annulé";
                    }

                    // 4. ENVOYER LE MESSAGE WEBSOCKET SEULEMENT UNE FOIS
                    if (newStatus != null) {
                        ExamStatusMessage wsMessage = new ExamStatusMessage(
                                exam.getAccessionNumber(),
                                exam.getPatient().getFirstName() + " " + exam.getPatient().getLastName(),
                                exam.getExamType().toString(),
                                oldStatus.toString(),
                                newStatus.toString(),
                                statusMessage
                        );

                        // Utiliser UNIQUEMENT webSocketService
                        webSocketService.sendStatusUpdate(wsMessage);
                        System.out.println("   📡 Message WebSocket envoyé");

                        // COMMENTEZ OU SUPPRIMEZ cette ligne
                        // notificationService.notifyExamStatusUpdate(exam, oldStatus, newStatus);

                        // 6. Mettre à jour le statut dans la base
                        exam.setStatus(newStatus);
                        examRepo.save(exam);
                        System.out.println("   💾 Examen sauvegardé avec le statut: " + newStatus);
                    }
                } else {
                    System.err.println("   ❌ Examen introuvable pour Accession: " + accessionNumber);
                }
            } catch (Exception e) {
                System.err.println("   ⚠️  Erreur lors du traitement MPPS : " + e.getMessage());
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