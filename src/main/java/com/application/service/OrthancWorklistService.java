package com.application.service;

import com.application.config.ApplicationProperties;
import com.application.entity.Exam;
import com.application.entity.ProcedureStep;
import com.application.repository.ExamRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OrthancWorklistService {

    private static final Logger logger = LoggerFactory.getLogger(OrthancWorklistService.class);
    private final String orthancWorklistBaseUrl;
    private final RestTemplate restTemplate;
    private final ExamRepository examRepo;
    private final ObjectMapper objectMapper;

    public OrthancWorklistService(ApplicationProperties applicationProperties, RestTemplate restTemplate, ExamRepository examRepo) {
        this.orthancWorklistBaseUrl = applicationProperties.getOrthancWorklistBaseUrl();
        this.restTemplate = restTemplate;
        this.examRepo = examRepo;
        this.objectMapper = new ObjectMapper();
    }

    public boolean sendWorklistToOrthanc(List<Exam> exams) {
        try {
            for (Exam exam : exams) {
                // Load exam with procedure steps to avoid lazy initialization exception
                Optional<Exam> examWithStepsOpt = examRepo.findByIdWithProcedureSteps(exam.getId());
                if (examWithStepsOpt.isEmpty()) {
                    logger.error("Exam not found with ID: {}", exam.getId());
                    return false;
                }
                Exam examWithSteps = examWithStepsOpt.get();
                
                // Create SEPARATE worklist entries for EACH procedure step (one SPS per worklist)
                // But ALL worklists will share the SAME StudyInstanceUID
                List<Map<String, Object>> worklistPayloads = convertExamToWorklistFormat(examWithSteps);
                
                for (Map<String, Object> worklistPayload : worklistPayloads) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    // Generate unique worklist ID for each SPS (e.g., WL-0099-1, WL-0099-2, etc.)
                    String worklistId = generateWorklistIdForStep(examWithSteps, worklistPayloads.indexOf(worklistPayload) + 1);
                    String worklistUrl = orthancWorklistBaseUrl + worklistId;
                    
                    // Log StudyInstanceUID being sent with null check
                    Map<String, Object> tags = (Map<String, Object>) worklistPayload.get("Tags");
                    Object studyUid = tags != null ? tags.get("StudyInstanceUID") : null;
                    logger.info("Sending worklist {} with StudyInstanceUID: {}", worklistId, studyUid);
                    
                    // Log the exact JSON being sent to Orthanc
                    try {
                        String jsonPayload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(worklistPayload);
                        logger.info("=== EXACT JSON SENT TO ORTHANC ===");
                        logger.info("Worklist ID: {}", worklistId);
                        logger.info("StudyInstanceUID in JSON: {}", studyUid);
                        logger.info("Full JSON:\n{}", jsonPayload);
                        logger.info("=== END EXACT JSON ===");
                    } catch (Exception e) {
                        logger.warn("Could not serialize worklist payload for logging: {}", e.getMessage());
                    }
                    
                    HttpEntity<Map<String, Object>> entity = new HttpEntity<>(worklistPayload, headers);
                    
                    try {
                        ResponseEntity<String> response = restTemplate.exchange(
                            worklistUrl,
                            HttpMethod.PUT,
                            entity,
                            String.class
                        );
                            
                        if (response.getStatusCode() == HttpStatus.OK) {
                                logger.info("Successfully sent worklist for exam: {} - SPS: {}", 
                                    examWithSteps.getAccessionNumber(), worklistId);
                            } else {
                                logger.error("Failed to send worklist for exam: {} - SPS: {}. Status: {}", 
                                    examWithSteps.getAccessionNumber(), worklistId, response.getStatusCode());
                                return false;
                            }
                    } catch (Exception e) {
                        logger.error("Failed to send worklist {} to Orthanc: {}", worklistId, e.getMessage());
                        return false;
                    }
                }
            }
            return true;
        } catch (Exception e) {
            logger.error("Error sending worklist to Orthanc", e);
            return false;
        }
    }

    private List<Map<String, Object>> convertExamToWorklistFormat(Exam exam) {
        List<Map<String, Object>> worklistEntries = new java.util.ArrayList<>();
        
        // IMPORTANT: Determine StudyInstanceUID ONCE for this exam - ALL worklists will share it
        String studyInstanceUID;
        logger.info("DEBUG: Exam ID: {}, AccessionNumber: {}", exam.getId(), exam.getAccessionNumber());
        logger.info("DEBUG: exam.getStudyInstanceUID() = {}", exam.getStudyInstanceUID());
        
        if (exam.getStudyInstanceUID() != null && !exam.getStudyInstanceUID().trim().isEmpty()) {
            studyInstanceUID = exam.getStudyInstanceUID();
            logger.info("Using exam StudyInstanceUID: {} for ALL worklists", studyInstanceUID);
        } else {
            // Fallback to a hardcoded StudyInstanceUID as string
            studyInstanceUID = "1.2.276.0.7230010.3.1.2.811872610.1.1771065366.591019";
            logger.warn("Exam has no StudyInstanceUID, using fallback: {} for ALL worklists", studyInstanceUID);
        }
        
        // Create SEPARATE worklist entry for EACH procedure step (one SPS per worklist)
        // But ALL worklists will share the SAME StudyInstanceUID
        if (exam.getProcedure() != null && exam.getProcedure().getProcedureSteps() != null && 
            !exam.getProcedure().getProcedureSteps().isEmpty()) {
            
            int stepIndex = 1;
            for (ProcedureStep step : exam.getProcedure().getProcedureSteps()) {
                // Include step if it's required or not completed (active steps)
                if (step.getIsRequired() != null && step.getIsRequired() || 
                    step.getIsCompleted() == null || !step.getIsCompleted()) {
                    
                    Map<String, Object> payload = new HashMap<>();
                    Map<String, Object> tags = new HashMap<>();
                    
                    // Patient information
                    tags.put("PatientName", exam.getPatient().getLastName() + "^" + exam.getPatient().getFirstName());
                    tags.put("PatientID", exam.getPatient().getPatientId());
                    tags.put("AccessionNumber", exam.getAccessionNumber());
                    
                    // Use the SAME StudyInstanceUID for ALL worklists
                    tags.put("StudyInstanceUID", studyInstanceUID);

                    // Create procedure step data
                    Map<String, Object> procedureStep = new HashMap<>();
                    
                    // Modality
                    procedureStep.put("Modality", exam.getModalityCode());
                    
                    // Station AE Title
                    String stationAETitle = "MODALITY_ROBOT";
                    if (exam.getModalityEntity() != null && exam.getModalityEntity().getAetitle() != null) {
                        stationAETitle = exam.getModalityEntity().getAetitle();
                    }
                    procedureStep.put("ScheduledStationAETitle", stationAETitle);
                    
                    // Station Name
                    String stationName = exam.getModalityCode() + "_ROOM_1";
                    if (exam.getModalityEntity() != null && exam.getModalityEntity().getRoom() != null && 
                        exam.getModalityEntity().getRoom().getName() != null) {
                        stationName = exam.getModalityEntity().getRoom().getName();
                    }
                    procedureStep.put("ScheduledStationName", stationName);
                    
                    // Procedure Step ID
                    procedureStep.put("ScheduledProcedureStepID", 
                        String.format("SPS-%s-%03d", exam.getModalityCode(), stepIndex));
                    
                    // Procedure Step Description
                    String description = step.getDescription() != null ? step.getDescription() : 
                        (exam.getProcedure().getName() != null ? exam.getProcedure().getName() : "Procedure Step " + stepIndex);
                    procedureStep.put("ScheduledProcedureStepDescription", description);
                    
                    // Date and Time - calculate based on step order and exam scheduled time
                    if (exam.getScheduledDateTime() != null) {
                        // Add 5 minutes per step to create a sequence
                        java.time.LocalDateTime stepTime = exam.getScheduledDateTime().plusMinutes((stepIndex - 1) * 5);
                        procedureStep.put("ScheduledProcedureStepStartDate", 
                            stepTime.format(DateTimeFormatter.BASIC_ISO_DATE));
                        procedureStep.put("ScheduledProcedureStepStartTime", 
                            stepTime.format(DateTimeFormatter.ofPattern("HHmmss")));
                    }

                    // Add SINGLE procedure step to sequence (one item per worklist)
                    List<Map<String, Object>> spsList = new ArrayList<>();
                    spsList.add(procedureStep);
                    tags.put("ScheduledProcedureStepSequence", spsList);
                    
                    payload.put("Tags", tags);
                    worklistEntries.add(payload);
                    stepIndex++;
                }
            }
        } else {
            // Fallback: create a single worklist entry if no steps are defined
            Map<String, Object> payload = new HashMap<>();
            Map<String, Object> tags = new HashMap<>();
            
            // Patient information
            tags.put("PatientName", exam.getPatient().getLastName() + "^" + exam.getPatient().getFirstName());
            tags.put("PatientID", exam.getPatient().getPatientId());
            tags.put("AccessionNumber", exam.getAccessionNumber());
            
            // Use the SAME StudyInstanceUID for ALL worklists
            tags.put("StudyInstanceUID", studyInstanceUID);

            // Create single procedure step
            Map<String, Object> procedureStep = new HashMap<>();
            procedureStep.put("Modality", exam.getModalityCode());
            
            String stationAETitle = "MODALITY_ROBOT";
            if (exam.getModalityEntity() != null && exam.getModalityEntity().getAetitle() != null) {
                stationAETitle = exam.getModalityEntity().getAetitle();
            }
            procedureStep.put("ScheduledStationAETitle", stationAETitle);
            
            String stationName = exam.getModalityCode() + "_ROOM_1";
            if (exam.getModalityEntity() != null && exam.getModalityEntity().getRoom() != null && 
                exam.getModalityEntity().getRoom().getName() != null) {
                stationName = exam.getModalityEntity().getRoom().getName();
            }
            procedureStep.put("ScheduledStationName", stationName);
            procedureStep.put("ScheduledProcedureStepID", "SPS-" + exam.getAccessionNumber());
            
            // Use procedure name or additional instructions for description
            String description = "";
            if (exam.getProcedure() != null && exam.getProcedure().getName() != null) {
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
                    exam.getExamType().toString() : exam.getModalityCode() + " Examination";
            }
            procedureStep.put("ScheduledProcedureStepDescription", description);
            
            if (exam.getScheduledDateTime() != null) {
                procedureStep.put("ScheduledProcedureStepStartDate", 
                    exam.getScheduledDateTime().format(DateTimeFormatter.BASIC_ISO_DATE));
                procedureStep.put("ScheduledProcedureStepStartTime", 
                    exam.getScheduledDateTime().format(DateTimeFormatter.ofPattern("HHmmss")));
            }
            
            // Add SINGLE procedure step to sequence (one item per worklist)
            List<Map<String, Object>> spsList = new ArrayList<>();
            spsList.add(procedureStep);
            tags.put("ScheduledProcedureStepSequence", spsList);
            
            payload.put("Tags", tags);
            worklistEntries.add(payload);
        }
        
        return worklistEntries;
    }
    
    private String generateWorklistIdForStep(Exam exam, int stepNumber) {
        // Use worklist attribute from exam if available, otherwise fallback to accession number
        String baseWorklistId = exam.getWorklist() != null ? exam.getWorklist() : exam.getAccessionNumber();
        // Add step number to make it unique (e.g., WL-0099-1, WL-0099-2, etc.)
        return baseWorklistId + "-" + stepNumber;
    }
  public List<Exam> getSelectedExams() {
        // This method should be implemented to get exams with SELECTED status
        // For now, returning empty list - will be implemented with ExamRepository
        return List.of();
    }
}
