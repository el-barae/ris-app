package com.application.dicom;

import com.application.entity.Exam;
import com.application.entity.ExamStatus;
import com.application.repository.ExamRepository;
import org.dcm4che3.data.*;
import org.dcm4che3.net.Association;
import org.dcm4che3.net.Commands;
import org.dcm4che3.net.Dimse;
import org.dcm4che3.net.Status;
import org.dcm4che3.net.pdu.PresentationContext;
import org.dcm4che3.net.service.BasicCFindSCP;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

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

        System.out.println("🔍 [MWL] Requête C-FIND reçue de: " + as.getCallingAET());

        if (dimse != Dimse.C_FIND_RQ) {
            throw new IOException("Commande DICOM non supportée : " + dimse);
        }

        try {
            // 1. Extraction des critères de recherche depuis la requête
            String requestedModality = null;
            String requestedStationAET = null;
            String requestedDate = null;

            // Lire les filtres depuis ScheduledProcedureStepSequence
            if (keys.contains(Tag.ScheduledProcedureStepSequence)) {
                Attributes spsItem = keys.getNestedDataset(Tag.ScheduledProcedureStepSequence);
                if (spsItem != null) {
                    requestedModality = spsItem.getString(Tag.Modality);
                    requestedStationAET = spsItem.getString(Tag.ScheduledStationAETitle);
                    requestedDate = spsItem.getString(Tag.ScheduledProcedureStepStartDate);
                }
            }

            System.out.println("📋 Critères de recherche:");
            System.out.println("   - Modality demandée: " + (requestedModality != null ? requestedModality : "TOUTES"));
            System.out.println("   - Station AET demandée: " + (requestedStationAET != null ? requestedStationAET : "TOUTES"));
            System.out.println("   - Date demandée: " + (requestedDate != null ? requestedDate : "AUJOURD'HUI"));

            // 2. Récupération des examens depuis la base
            List<Exam> allExams = examRepository.findAllWithRelations();
            System.out.println("   -> Examens en base: " + allExams.size());

            // 3. Filtrage des examens
            List<Exam> filteredExams = filterExams(allExams, requestedModality, requestedStationAET, requestedDate);
            System.out.println("   -> Examens correspondants: " + filteredExams.size());

            // 4. Envoi des résultats
            int sentCount = 0;
            for (Exam exam : filteredExams) {
                if (exam == null || exam.getPatient() == null) {
                    System.out.println("   ⚠️  Examen ignoré (Données incomplètes)");
                    continue;
                }

                Attributes mwlItem = createMwlResponse(exam);
                as.writeDimseRSP(pc, Commands.mkCFindRSP(cmd, Status.Pending), mwlItem);

                sentCount++;
                System.out.println("   ✅ Envoyé: " + exam.getPatient().getLastName() +
                        " - " + exam.getModality() +
                        " - " + requestedStationAET);
            }

            // 5. Fin de la transmission
            as.writeDimseRSP(pc, Commands.mkCFindRSP(cmd, Status.Success), null);
            System.out.println("✅ C-FIND terminé: " + sentCount + " résultat(s) envoyé(s)");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du traitement C-FIND: " + e.getMessage());
            e.printStackTrace();
            as.writeDimseRSP(pc, Commands.mkCFindRSP(cmd, Status.ProcessingFailure), null);
        }
    }

    /**
     * Filtre les examens selon les critères de la modalité
     */
    private List<Exam> filterExams(List<Exam> exams, String modality, String stationAET, String date) {

        // Convertir la date DICOM en LocalDate
        LocalDate searchDate = null;
        if (date != null && !date.isEmpty() && date.length() == 8) {
            try {
                searchDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyyMMdd"));
            } catch (Exception e) {
                System.err.println("⚠️  Date invalide: " + date);
                searchDate = LocalDate.now();
            }
        } else {
            searchDate = LocalDate.now();
        }

        final LocalDate finalSearchDate = searchDate;

        return exams.stream()
                .filter(exam -> {
                    // Filtre 1: Statut SELECTED uniquement
                    if (exam.getStatus() != ExamStatus.SELECTED) {
                        return false;
                    }

                    // Filtre 2: Modalité
                    if (modality != null && !modality.isEmpty() && !modality.equals("*")) {
                        String examModality = exam.getModality();
                        if (examModality == null || !examModality.equalsIgnoreCase(modality)) {
                            System.out.println("   ❌ Filtré (modalité): " + exam.getAccessionNumber() +
                                    " (" + examModality + " != " + modality + ")");
                            return false;
                        }
                    }

                    // Filtre 3: Station AET - On compare avec le AE Title demandant
                    // Pour l'instant, on accepte tous les examens de la bonne modalité
                    // Vous pouvez ajouter un champ scheduledStationAET dans votre base si besoin

                    // Filtre 4: Date
//                    if (finalSearchDate != null && exam.getScheduledDateTime() != null) {
//                        LocalDate examDate = exam.getScheduledDateTime().toLocalDate();
//                        if (!examDate.equals(finalSearchDate)) {
//                            System.out.println("   ❌ Filtré (date): " + exam.getAccessionNumber() +
//                                    " (" + examDate + " != " + finalSearchDate + ")");
//                            return false;
//                        }
//                    }

                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * Crée la réponse MWL pour un examen
     */
    private Attributes createMwlResponse(Exam exam) {
        Attributes mwlItem = new Attributes();

        // --- 1. Info Patient ---
        String lastName = exam.getPatient().getLastName() != null ? exam.getPatient().getLastName() : "INCONNU";
        String firstName = exam.getPatient().getFirstName() != null ? exam.getPatient().getFirstName() : "";
        mwlItem.setString(Tag.PatientName, VR.PN, lastName + "^" + firstName);

        mwlItem.setString(Tag.PatientID, VR.LO, exam.getPatient().getPatientId());
        mwlItem.setString(Tag.PatientSex, VR.CS, exam.getPatient().getGender().toString());

        if (exam.getPatient().getDateOfBirth() != null) {
            mwlItem.setString(Tag.PatientBirthDate, VR.DA,
                    exam.getPatient().getDateOfBirth().format(daFormat));
        }

        // --- 2. Info Examen ---
        mwlItem.setString(Tag.AccessionNumber, VR.SH, exam.getAccessionNumber());

        // StudyInstanceUID
        String studyUID = exam.getStudyInstanceUID();
        if (studyUID == null || studyUID.isEmpty()) {
            studyUID = "1.2.840.10008.5.1.4.1.1.1." + exam.getId();
        }
        mwlItem.setString(Tag.StudyInstanceUID, VR.UI, studyUID);

        mwlItem.setString(Tag.RequestedProcedureDescription, VR.LO, exam.getExamType().toString());

        // --- 3. Scheduled Procedure Step Sequence ---
        Sequence spsSeq = mwlItem.newSequence(Tag.ScheduledProcedureStepSequence, 1);
        Attributes spsItem = new Attributes();

        // ID du SPS
        spsItem.setString(Tag.ScheduledProcedureStepID, VR.SH, "SPS-" + exam.getId());

        // MODALITÉ - CRUCIAL pour le filtrage
        spsItem.setString(Tag.Modality, VR.CS, exam.getModality());

        // STATION AET - Utilise le AET de la modalité qui demande
        // Si vous avez un champ dans la base, utilisez-le, sinon on met une valeur générique
        spsItem.setString(Tag.ScheduledStationAETitle, VR.AE, "ANY-MODALITY");

        // Date et heure
        if (exam.getScheduledDateTime() != null) {
            spsItem.setString(Tag.ScheduledProcedureStepStartDate, VR.DA,
                    exam.getScheduledDateTime().format(daFormat));
            spsItem.setString(Tag.ScheduledProcedureStepStartTime, VR.TM,
                    exam.getScheduledDateTime().format(tmFormat));
        }

        // Description
        String desc = "";
        if (exam.getProcedure() != null) {
            desc = exam.getProcedure().getName();
        }
        if (exam.getAdditionalInstructions() != null && !exam.getAdditionalInstructions().trim().isEmpty()) {
            if (!desc.isEmpty()) {
                desc += " - ";
            }
            desc += exam.getAdditionalInstructions();
        }
        if (desc.isEmpty()) {
            desc = exam.getExamType() != null ? 
                exam.getExamType().toString() : exam.getModality() + " Examination";
        }
        spsItem.setString(Tag.ScheduledProcedureStepDescription, VR.LO, desc);

        // Médecin
        if (exam.getMedecin() != null) {
            String physicianName = exam.getMedecin().getLastName() + "^" + exam.getMedecin().getFirstName();
            spsItem.setString(Tag.ScheduledPerformingPhysicianName, VR.PN, physicianName);
        }

        spsSeq.add(spsItem);

        return mwlItem;
    }
}