package com.application.service;

import com.application.config.ApplicationProperties;
import com.application.entity.Exam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrthancWorklistService {

    private static final Logger logger = LoggerFactory.getLogger(OrthancWorklistService.class);
    private final String orthancWorklistBaseUrl;
    
    @Autowired
    private RestTemplate restTemplate;

    public OrthancWorklistService(ApplicationProperties applicationProperties, RestTemplate restTemplate) {
        this.orthancWorklistBaseUrl = applicationProperties.getOrthancWorklistBaseUrl();
        this.restTemplate = restTemplate;
    }

    public boolean sendWorklistToOrthanc(List<Exam> exams) {
        try {
            for (Exam exam : exams) {
                Map<String, Object> worklistPayload = convertExamToWorklistFormat(exam);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(worklistPayload, headers);

                // Use accession number as worklist ID
                String worklistUrl = orthancWorklistBaseUrl + exam.getAccessionNumber();
                
                logger.info("Sending worklist to Orthanc for exam: {}", exam.getAccessionNumber());
                
                ResponseEntity<String> response = restTemplate.exchange(
                    worklistUrl,
                    HttpMethod.PUT,
                    entity,
                    String.class
                );
                
                if (response.getStatusCode() == HttpStatus.OK) {
                    logger.info("Successfully sent worklist for exam: {}", exam.getAccessionNumber());
                } else {
                    logger.error("Failed to send worklist for exam: {}. Status: {}", 
                        exam.getAccessionNumber(), response.getStatusCode());
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("Error sending worklist to Orthanc", e);
            return false;
        }
    }

    private Map<String, Object> convertExamToWorklistFormat(Exam exam) {
        Map<String, Object> payload = new HashMap<>();
        Map<String, Object> tags = new HashMap<>();
        
        // Patient information
        if (exam.getPatient() != null) {
            tags.put("PatientID", exam.getPatient().getPatientId());
            tags.put("PatientName", exam.getPatient().getLastName() + "^" + exam.getPatient().getFirstName());
            
            // Convert Gender enum to DICOM format (M/F/O)
            String sex = "O"; // Default to Other/Unknown
            if (exam.getPatient().getGender() != null) {
                sex = switch (exam.getPatient().getGender()) {
                    case MALE -> "M";
                    case FEMALE -> "F";
                };
            }
            tags.put("PatientSex", sex);
            
            if (exam.getPatient().getDateOfBirth() != null) {
                tags.put("PatientBirthDate", exam.getPatient().getDateOfBirth().format(DateTimeFormatter.BASIC_ISO_DATE));
            }
        }
        
        // Exam information
        tags.put("AccessionNumber", exam.getAccessionNumber());
        tags.put("InstitutionName", "UMAN");
        tags.put("SpecificCharacterSet", "ISO_IR 192");
        
        if (exam.getStudyInstanceUID() != null) {
            tags.put("StudyInstanceUID", exam.getStudyInstanceUID());
        }
        
        // Scheduled procedure step
        Map<String, Object> procedureStep = new HashMap<>();
        procedureStep.put("Modality", exam.getModalityCode());
        
        // Use actual AE title from modality if available, otherwise fallback
        String stationAETitle = "MODALITY_ROBOT";
        if (exam.getModalityEntity() != null && exam.getModalityEntity().getAetitle() != null) {
            stationAETitle = exam.getModalityEntity().getAetitle();
        }
        procedureStep.put("ScheduledStationAETitle", stationAETitle);
        
        // Use actual room name if available, otherwise fallback
        String stationName = exam.getModalityCode() + "_ROOM_1";
        if (exam.getModalityEntity() != null && exam.getModalityEntity().getRoom() != null && 
            exam.getModalityEntity().getRoom().getName() != null) {
            stationName = exam.getModalityEntity().getRoom().getName();
        }
        procedureStep.put("ScheduledStationName", stationName);
        procedureStep.put("ScheduledProcedureStepID", "SPS-" + exam.getAccessionNumber());

        // Utiliser la procédure et les instructions additionnelles pour la description
        String description = "";
        if (exam.getProcedure() != null) {
            description = exam.getProcedure().getName();
        }
        if (exam.getAdditionalInstructions() != null && !exam.getAdditionalInstructions().trim().isEmpty()) {
            if (!description.isEmpty()) {
                description += " - ";
            }
            description += exam.getAdditionalInstructions();
        }
        if (description.isEmpty()) {
            description = exam.getExamType() != null ? 
                exam.getExamType().toString() : exam.getModality() + " Examination";
        }
        procedureStep.put("ScheduledProcedureStepDescription", description);
        
        if (exam.getScheduledDateTime() != null) {
            procedureStep.put("ScheduledProcedureStepStartDate", 
                exam.getScheduledDateTime().format(DateTimeFormatter.BASIC_ISO_DATE));
            procedureStep.put("ScheduledProcedureStepStartTime", 
                exam.getScheduledDateTime().format(DateTimeFormatter.ofPattern("HHmmss")));
        }
        
        tags.put("ScheduledProcedureStepSequence", new Object[]{procedureStep});
        
        payload.put("Tags", tags);
        
        return payload;
    }

    public List<Exam> getSelectedExams() {
        // This method should be implemented to get exams with SELECTED status
        // For now, returning empty list - will be implemented with ExamRepository
        return List.of();
    }
}
