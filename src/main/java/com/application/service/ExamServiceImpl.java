package com.application.service;

import com.application.entity.*;
import com.application.repository.ExamRepository;
import com.application.repository.PatientRepository;
import com.application.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
@Transactional
public class ExamServiceImpl implements ExamService {

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UserRepository userRepository;

    private final Random random = new Random();

    @Override
    public Exam createExam(Exam exam) {
        // Validation des données
        validateExamData(exam);
        
        // Vérification que le patient existe
        if (exam.getPatient() == null || exam.getPatient().getId() == null) {
            throw new IllegalArgumentException("Patient is required");
        }
        patientRepository.findById(exam.getPatient().getId())
                .orElseThrow(() -> new PatientNotFoundException("Patient not found with id: " + exam.getPatient().getId()));
        
        // Vérification que le médecin existe
        if (exam.getMedecin() == null || exam.getMedecin().getId() == null) {
            throw new IllegalArgumentException("Medecin is required");
        }
        userRepository.findById(exam.getMedecin().getId())
                .orElseThrow(() -> new UserNotFoundException("Medecin not found with id: " + exam.getMedecin().getId()));

        // Génération automatique de l'accessionNumber si non fourni
        if (exam.getAccessionNumber() == null || exam.getAccessionNumber().trim().isEmpty()) {
            exam.setAccessionNumber(generateAccessionNumber());
        } else {
            // Vérification que l'accessionNumber est unique
            if (examRepository.findByAccessionNumber(exam.getAccessionNumber()).isPresent()) {
                throw new IllegalArgumentException("Accession number already exists: " + exam.getAccessionNumber());
            }
        }

        // Génération du studyInstanceUID si non fourni
        if (exam.getStudyInstanceUID() == null || exam.getStudyInstanceUID().trim().isEmpty()) {
            exam.setStudyInstanceUID(generateStudyInstanceUID());
        }

        // Génération du worklist si non fourni
        if (exam.getWorklist() == null || exam.getWorklist().trim().isEmpty()) {
            exam.setWorklist(generateWorklist());
        }

        // Calcul automatique de la modalité
        calculateModality(exam);

        return examRepository.save(exam);
    }

    @Override
    public Exam updateExam(Long id, Exam exam) {
        Exam existingExam = findById(id);
        
        // Validation des données
        validateExamData(exam);
        
        // Mise à jour des champs
        existingExam.setPatient(exam.getPatient());
        existingExam.setMedecin(exam.getMedecin());
        existingExam.setExamType(exam.getExamType());
        existingExam.setScheduledDateTime(exam.getScheduledDateTime());
        existingExam.setProcedure(exam.getProcedure());
        existingExam.setAdditionalInstructions(exam.getAdditionalInstructions());
        existingExam.setPriority(exam.getPriority());

        // Recalcul de la modalité si le type a changé
        calculateModality(existingExam);

        return examRepository.save(existingExam);
    }

    @Override
    public void deleteExam(Long id) {
        if (!examRepository.existsById(id)) {
            throw new ExamNotFoundException("Exam not found with id: " + id);
        }
        examRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Exam findById(Long id) {
        return examRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ExamNotFoundException("Exam not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Exam> findAll() {
        return examRepository.findAllWithRelations();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Exam> findByPatient(Long patientId) {
        return examRepository.findByPatientWithRelations(patientId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Exam> findByStatus(ExamStatus status) {
        return examRepository.findByStatusWithRelations(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Exam> findByMedecin(Long medecinId) {
        return examRepository.findByMedecinWithRelations(medecinId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Exam> findScheduledExams(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);
        return examRepository.findByScheduledDateTimeBetweenWithRelations(startOfDay, endOfDay);
    }

    @Override
    public Exam updateStatus(Long id, ExamStatus newStatus) {
        Exam exam = findById(id);
        exam.setStatus(newStatus);
        
        // Mise à jour automatique de performedDateTime quand status = COMPLETED
        if (newStatus == ExamStatus.COMPLETED && exam.getPerformedDateTime() == null) {
            exam.setPerformedDateTime(LocalDateTime.now());
        }
        
        return examRepository.save(exam);
    }

    @Override
    public String generateAccessionNumber() {
        String accessionNumber;
        int maxAttempts = 100;
        int attempts = 0;
        
        do {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String randomDigits = String.format("%04d", random.nextInt(10000));
            accessionNumber = "ACC" + timestamp + randomDigits;
            attempts++;
            
            if (attempts > maxAttempts) {
                throw new RuntimeException("Failed to generate unique accession number after " + maxAttempts + " attempts");
            }
        } while (examRepository.findByAccessionNumber(accessionNumber).isPresent());
        
        return accessionNumber;
    }

    @Override
    public String generateStudyInstanceUID() {
        // Format DICOM UID: <root UID>.<timestamp>.<random>
        String rootUid = "1.2.840.113619.2.55.3"; // Exemple de root UID
        String timestamp = String.valueOf(System.currentTimeMillis());
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return rootUid + "." + timestamp + "." + randomPart;
    }

    @Override
    public String generateWorklist() {
        // Get the maximum existing worklist number and increment
        String maxWorklist = examRepository.findMaxWorklist();
        int nextSequence = 1;
        
        if (maxWorklist != null && maxWorklist.startsWith("WL-")) {
            try {
                String numberPart = maxWorklist.substring(3); // Remove "WL-"
                nextSequence = Integer.parseInt(numberPart) + 1;
            } catch (NumberFormatException e) {
                // If parsing fails, start from 1
                nextSequence = 1;
            }
        }
        
        String worklist = "WL-" + String.format("%03d", nextSequence);
        
        // Double-check uniqueness (should be unique with this approach)
        if (examRepository.findByWorklist(worklist).isPresent()) {
            // Fallback: use timestamp-based approach
            long timestamp = System.currentTimeMillis();
            worklist = "WL-" + String.format("%03d", timestamp % 9999);
            
            // Final uniqueness check
            int attempts = 0;
            while (examRepository.findByWorklist(worklist).isPresent() && attempts < 100) {
                timestamp++;
                worklist = "WL-" + String.format("%03d", timestamp % 9999);
                attempts++;
            }
            
            if (attempts >= 100) {
                throw new RuntimeException("Failed to generate unique worklist after 100 attempts");
            }
        }
        
        return worklist;
    }

    private void validateExamData(Exam exam) {
        // Validation de l'ordre
        if (exam.getOrder() == null) {
            throw new IllegalArgumentException("Order is required");
        }

        // Validation du patient via l'ordre
        if (exam.getOrder().getPatient() == null) {
            throw new IllegalArgumentException("Patient is required");
        }

        // Validation du médecin via l'ordre
        if (exam.getOrder().getDoctor() == null) {
            throw new IllegalArgumentException("Medecin is required");
        }

        // Validation du type de modalité
        if (exam.getModalityType() == null) {
            throw new IllegalArgumentException("Modality type is required");
        }

        // Validation de la procédure
        if (exam.getProcedure() == null) {
            throw new IllegalArgumentException("Procedure is required");
        }

        // Validation de la date programmée
        if (exam.getScheduledDateTime() == null) {
            throw new IllegalArgumentException("Scheduled date time is required");
        }
        
        if (exam.getScheduledDateTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Scheduled date time cannot be in the past");
        }

        // Validation du statut
        if (exam.getStatus() == null) {
            throw new IllegalArgumentException("Status is required");
        }

        // Validation de la priorité
        if (exam.getPriority() == null) {
            throw new IllegalArgumentException("Priority is required");
        }
    }

    private void calculateModality(Exam exam) {
        if (exam.getExamType() != null) {
            switch (exam.getExamType()) {
                case CT:
                    exam.setModality("CT");
                    break;
                case MRI:
                    exam.setModality("MR");
                    break;
                case RX:
                    exam.setModality("XR");
                    break;
                case ECHO:
                    exam.setModality("US");
                    break;
                case MAMMO:
                    exam.setModality("MG");
                    break;
                case FLUORO:
                    exam.setModality("RF");
                    break;
                case PET:
                    exam.setModality("PT");
                    break;
                default:
                    exam.setModality(exam.getExamType().name());
            }
        }
    }
}
