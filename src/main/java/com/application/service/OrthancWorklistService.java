package com.application.service;

import com.application.config.ApplicationProperties;
import com.application.entity.Exam;
import com.application.entity.ProcedureStep;
import com.application.repository.ExamRepository;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OrthancWorklistService {

    private static final Logger logger = LoggerFactory.getLogger(OrthancWorklistService.class);
    private final String orthancWorklistBaseUrl;
    private final RestTemplate restTemplate;
    private final ExamRepository examRepo;

    public OrthancWorklistService(ApplicationProperties applicationProperties, RestTemplate restTemplate, ExamRepository examRepo) {
        this.orthancWorklistBaseUrl = applicationProperties.getOrthancWorklistBaseUrl();
        this.restTemplate = restTemplate;
        this.examRepo = examRepo;
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
                
                Map<String, Object> worklistPayload = convertExamToWorklistFormat(examWithSteps);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(worklistPayload, headers);

                // Use worklist attribute from exam if available, otherwise fallback to accession number
                String worklistId = examWithSteps.getWorklist() != null ? examWithSteps.getWorklist() : examWithSteps.getAccessionNumber();
                String worklistUrl = orthancWorklistBaseUrl + worklistId;
                
                logger.info("Sending worklist to Orthanc for exam: {}", examWithSteps.getAccessionNumber());
                
                ResponseEntity<String> response = restTemplate.exchange(
                    worklistUrl,
                    HttpMethod.PUT,
                    entity,
                    String.class
                );
                
                if (response.getStatusCode() == HttpStatus.OK) {
                    logger.info("Successfully sent worklist for exam: {}", examWithSteps.getAccessionNumber());
                } else {
                    logger.error("Failed to send worklist for exam: {}. Status: {}", 
                        examWithSteps.getAccessionNumber(), response.getStatusCode());
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
        
        // Create ScheduledProcedureStepSequence from procedure steps
        List<Map<String, Object>> procedureSteps = createProcedureStepsFromExam(exam);
        tags.put("ScheduledProcedureStepSequence", procedureSteps.toArray());
        
        payload.put("Tags", tags);
        
        return payload;
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
            
            procedureSteps.add(procedureStep);
        }
        
        return procedureSteps;
    }

    public List<Exam> getSelectedExams() {
        // This method should be implemented to get exams with SELECTED status
        // For now, returning empty list - will be implemented with ExamRepository
        return List.of();
    }
}
