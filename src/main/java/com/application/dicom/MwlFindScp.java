package com.application.dicom;

import com.application.entity.Exam;
import com.application.entity.ExamStatus;
import com.application.repository.ExamRepository;
import org.dcm4che3.data.*;
import org.dcm4che3.net.*;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicCFindSCP;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MwlFindScp extends BasicCFindSCP {

    private final ExamRepository examRepository;
    private final DateTimeFormatter daFormat = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final DateTimeFormatter tmFormat = DateTimeFormatter.ofPattern("HHmmss");

    public MwlFindScp(ExamRepository repo) {
        super(UID.ModalityWorklistInformationModelFind);
        this.examRepository = repo;
    }

    @Override
    public void onDimseRQ(Association as, PresentationContext pc, Dimse dimse,
                          Attributes cmd, Attributes keys) throws IOException {

        System.out.println(" [SCP] Requête Worklist (C-FIND) reçue...");

        if (dimse != Dimse.C_FIND_RQ) {
            throw new IOException("Commande DICOM non supportée : " + dimse);
        }

        try {
            // 1. Récupération des données avec les relations (patient et medecin)
            List<Exam> exams = examRepository.findAllWithRelations();
            System.out.println("   -> Nombre d'examens en base : " + exams.size());

            for (Exam exam : exams) {
                // Sécurité : Si l'examen ou le patient est null, on saute
                if (exam == null || exam.getPatient() == null) {
                    System.out.println("   Examen ignoré (Données incomplètes)");
                    continue;
                }

                // Filtrer uniquement les examens avec statut SELECTED
                if (exam.getStatus() != ExamStatus.SELECTED) {
                    continue;
                }

                Attributes mwlItem = new Attributes();

                // --- 1. Info Patient ---
                String lastName = (exam.getPatient().getLastName() != null) ? exam.getPatient().getLastName() : "INCONNU";
                String firstName = (exam.getPatient().getFirstName() != null) ? exam.getPatient().getFirstName() : "";
                mwlItem.setString(Tag.PatientName, VR.PN, lastName + "^" + firstName);

                mwlItem.setString(Tag.PatientID, VR.LO, exam.getPatient().getPatientId());
                mwlItem.setString(Tag.PatientSex, VR.CS, exam.getPatient().getGender().toString());

                if (exam.getPatient().getDateOfBirth() != null) {
                    mwlItem.setString(Tag.PatientBirthDate, VR.DA, exam.getPatient().getDateOfBirth().format(daFormat));
                }

                // --- 2. Info Examen ---
                mwlItem.setString(Tag.AccessionNumber, VR.SH, exam.getAccessionNumber());
                mwlItem.setString(Tag.StudyInstanceUID, VR.UI, "1.2.840.10008.5.1.4.1.1.1." + exam.getId());
                mwlItem.setString(Tag.RequestedProcedureDescription, VR.LO, exam.getExamType().toString());

                // --- 3. Info SPS ---
                Sequence spsSeq = mwlItem.newSequence(Tag.ScheduledProcedureStepSequence, 1);
                Attributes spsItem = new Attributes();

                spsItem.setString(Tag.ScheduledProcedureStepID, VR.SH, "SPS-" + exam.getId());
                spsItem.setString(Tag.Modality, VR.CS, exam.getModality());
                spsItem.setString(Tag.ScheduledStationAETitle, VR.AE, "ANY-MODALITY");

                // Gestion sécurisée des dates
                if (exam.getScheduledDateTime() != null) {
                    spsItem.setString(Tag.ScheduledProcedureStepStartDate, VR.DA, exam.getScheduledDateTime().format(daFormat));
                    spsItem.setString(Tag.ScheduledProcedureStepStartTime, VR.TM, exam.getScheduledDateTime().format(tmFormat));
                }

                // Description technique (Instructions, etc.)
                String desc = (exam.getInstructions() != null) ? exam.getInstructions() : "";
                spsItem.setString(Tag.ScheduledProcedureStepDescription, VR.LO, desc);

                spsSeq.add(spsItem);

                // Envoi de la réponse partielle
                as.writeDimseRSP(pc, Commands.mkCFindRSP(cmd, Status.Pending), mwlItem);
                System.out.println("   Worklist envoyée pour : " + lastName);
            }

            // Fin de la transmission
            as.writeDimseRSP(pc, Commands.mkCFindRSP(cmd, Status.Success), null);

        } catch (Exception e) {
            System.err.println(" Erreur lors du traitement C-FIND : " + e.getMessage());
            e.printStackTrace(); // Affiche l'erreur dans la console du serveur
            // En cas d'erreur, on essaie de fermer proprement pour ne pas faire planter le client
            as.writeDimseRSP(pc, Commands.mkCFindRSP(cmd, Status.ProcessingFailure), null);
        }
    }
}