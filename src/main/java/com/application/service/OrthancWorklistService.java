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
                
                // Create separate worklist entries for each procedure step
                List<Map<String, Object>> worklistPayloads = convertExamToWorklistFormat(examWithSteps);
                
                for (Map<String, Object> worklistPayload : worklistPayloads) {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    // Generate unique worklist ID for each SPS
                    String worklistId = generateWorklistId(examWithSteps, worklistPayload);
                    String worklistUrl = orthancWorklistBaseUrl + worklistId;
                    
                    // Log the exact JSON being sent and also create a Postman-compatible version
                    try {
                        String jsonPayload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(worklistPayload);
                        logger.info("=== WORKLIST PAYLOAD DEBUG ===");
                        logger.info("Worklist ID: {}", worklistId);
                        logger.info("JSON Payload:\n{}", jsonPayload);
                        
                        // Create exact Postman format for comparison
                        Map<String, Object> postmanFormat = new HashMap<>();
                        Map<String, Object> postmanTags = new HashMap<>();
                        
                        // Extract tags safely
                        Object tagsObj = worklistPayload.get("Tags");
                        if (tagsObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> originalTags = (Map<String, Object>) tagsObj;
                            
                            // Copy only essential fields like Postman
                            postmanTags.put("PatientName", originalTags.get("PatientName"));
                            postmanTags.put("PatientID", originalTags.get("PatientID"));
                            postmanTags.put("AccessionNumber", originalTags.get("AccessionNumber"));
                            postmanTags.put("ScheduledProcedureStepSequence", originalTags.get("ScheduledProcedureStepSequence"));
                        }
                        
                        postmanFormat.put("Tags", postmanTags);
                        
                        String postmanJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(postmanFormat);
                        logger.info("=== POSTMAN FORMAT COMPARISON ===");
                        logger.info("Postman-style JSON:\n{}", postmanJson);
                        logger.info("=== END DEBUG ===");
                        
                        // Try sending Postman format instead
                        HttpEntity<Map<String, Object>> postmanEntity = new HttpEntity<>(postmanFormat, headers);
                        
                        logger.info("Sending worklist to Orthanc for exam: {} - SPS: {}", 
                            examWithSteps.getAccessionNumber(), worklistId);
                        
                        ResponseEntity<String> response = restTemplate.exchange(
                            worklistUrl,
                            HttpMethod.PUT,
                            postmanEntity,
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
                        logger.warn("Could not serialize worklist payload for logging: {}", e.getMessage());
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
        
        // Create separate worklist entry for each procedure step
        List<Map<String, Object>> procedureSteps = createProcedureStepsFromExam(exam);
        
        for (Map<String, Object> procedureStep : procedureSteps) {
            Map<String, Object> payload = new HashMap<>();
            Map<String, Object> tags = new HashMap<>();
            
            // Create minimal JSON structure matching Postman exactly
            tags.put("PatientName", exam.getPatient().getLastName() + "^" + exam.getPatient().getFirstName());
            tags.put("PatientID", exam.getPatient().getPatientId());
            tags.put("AccessionNumber", exam.getAccessionNumber());
            
            if (exam.getStudyInstanceUID() != null) {
                tags.put("StudyInstanceUID", exam.getStudyInstanceUID());
            } else {
                // Fallback to a hardcoded StudyInstanceUID as string
                tags.put("StudyInstanceUID", "1.2.276.0.7230010.3.1.2.811872610.1.1771065366.591019");
            }

            // Add single procedure step to sequence - use explicit List for proper JSON serialization
            List<Map<String, Object>> spsList = new ArrayList<>();
            spsList.add(procedureStep);
            tags.put("ScheduledProcedureStepSequence", spsList);
            
            payload.put("Tags", tags);
            worklistEntries.add(payload);
        }
        
        return worklistEntries;
    }
    
    private List<Map<String, Object>> createProcedureStepsFromExam(Exam exam) {
        List<Map<String, Object>> procedureSteps = new java.util.ArrayList<>();
        
        if (exam.getProcedure() != null && exam.getProcedure().getProcedureSteps() != null && 
            !exam.getProcedure().getProcedureSteps().isEmpty()) {
            
            // Use actual procedure steps from exam's procedure
            int stepIndex = 1;
            for (ProcedureStep step : exam.getProcedure().getProcedureSteps()) {
                // Include step if it's required or not completed (active steps)
                if (step.getIsRequired() != null && step.getIsRequired() || 
                    step.getIsCompleted() == null || !step.getIsCompleted()) {
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
                    
                    // Description - avoid empty descriptions like in Postman example
                    String description = step.getDescription() != null ? step.getDescription() : 
                        (exam.getProcedure() != null && exam.getProcedure().getName() != null ? 
                            exam.getProcedure().getName() : exam.getModalityCode() + " EXAM");
                    if (description.trim().isEmpty()) {
                        description = exam.getModalityCode() + " EXAM";
                    }
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
                    
                    procedureSteps.add(procedureStep);
                    stepIndex++;
                }
            }
        } else {
            // Fallback: create a single procedure step if no steps are defined
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
            
            // Use procedure name or additional instructions for description - avoid empty descriptions
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
            if (description.trim().isEmpty()) {
                description = exam.getModalityCode() + " EXAM";
            }
            procedureStep.put("ScheduledProcedureStepDescription", description);
            
            if (exam.getScheduledDateTime() != null) {
                procedureStep.put("ScheduledProcedureStepStartDate", 
                    exam.getScheduledDateTime().format(DateTimeFormatter.BASIC_ISO_DATE));
                procedureStep.put("ScheduledProcedureStepStartTime", 
                    exam.getScheduledDateTime().format(DateTimeFormatter.ofPattern("HHmmss")));
            }
            
            procedureSteps.add(procedureStep);
        }
        
        return procedureSteps;
    }

    private String generateWorklistId(Exam exam, Map<String, Object> worklistPayload) {
        // Extract SPS ID from the worklist payload to create unique worklist ID
        Map<String, Object> tags = (Map<String, Object>) worklistPayload.get("Tags");
        if (tags != null && tags.containsKey("ScheduledProcedureStepSequence")) {
            Object spsSequenceObj = tags.get("ScheduledProcedureStepSequence");
            if (spsSequenceObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> spsSequence = (List<Map<String, Object>>) spsSequenceObj;
                if (!spsSequence.isEmpty()) {
                    Map<String, Object> sps = spsSequence.get(0);
                    String spsId = (String) sps.get("ScheduledProcedureStepID");
                    if (spsId != null) {
                        return exam.getAccessionNumber() + "-" + spsId;
                    }
                }
            }
        }
        
        // Fallback to exam worklist or accession number
        return exam.getWorklist() != null ? exam.getWorklist() : exam.getAccessionNumber();
    }

    public List<Exam> getSelectedExams() {
        // This method should be implemented to get exams with SELECTED status
        // For now, returning empty list - will be implemented with ExamRepository
        return List.of();
    }
}
