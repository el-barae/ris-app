package com.application.service;

import com.application.entity.*;
import com.application.repository.ExamRepository;
import com.application.repository.ReportRepository;
import com.application.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Report createReport(Report report) {
        // Validation des données
        validateReportData(report);
        
        // Vérification que l'examen existe
        if (report.getExam() == null || report.getExam().getId() == null) {
            throw new IllegalArgumentException("Exam is required");
        }
        Exam exam = examRepository.findById(report.getExam().getId())
                .orElseThrow(() -> new ExamNotFoundException("Exam not found with id: " + report.getExam().getId()));
        
        // Validation : un exam ne peut avoir qu'un seul report
        if (reportRepository.findByExam(exam).isPresent()) {
            throw new IllegalArgumentException("A report already exists for this exam: " + exam.getAccessionNumber());
        }

        // Validation : seul un RADIOLOGUE peut créer un report
        if (report.getRadiologue() == null || report.getRadiologue().getId() == null) {
            throw new IllegalArgumentException("Radiologue is required");
        }
        User radiologue = userRepository.findById(report.getRadiologue().getId())
                .orElseThrow(() -> new UserNotFoundException("Radiologue not found with id: " + report.getRadiologue().getId()));
        
        if (radiologue.getRole() != UserRole.RADIOLOGUE) {
            throw new IllegalArgumentException("Only RADIOLOGUE can create reports. User role: " + radiologue.getRole());
        }

        // Initialisation du report
        report.setValidated(false);
        report.setValidatedAt(null);

        return reportRepository.save(report);
    }

    @Override
    public Report updateReport(Long id, Report report) {
        Report existingReport = findById(id);
        
        // Validation : un report validé ne peut plus être modifié (sauf par ADMIN)
        if (existingReport.getValidated() && existingReport.getRadiologue().getRole() != UserRole.ADMIN) {
            throw new IllegalArgumentException("Validated report cannot be modified");
        }
        
        // Validation des données
        validateReportData(report);

        // Mise à jour des champs
        existingReport.setFindings(report.getFindings());
        existingReport.setConclusion(report.getConclusion());
        
        // Si le radiologue est modifié, validation du nouveau rôle
        if (report.getRadiologue() != null && report.getRadiologue().getId() != null) {
            User newRadiologue = userRepository.findById(report.getRadiologue().getId())
                    .orElseThrow(() -> new UserNotFoundException("Radiologue not found with id: " + report.getRadiologue().getId()));
            
            if (newRadiologue.getRole() != UserRole.RADIOLOGUE && newRadiologue.getRole() != UserRole.ADMIN) {
                throw new IllegalArgumentException("Only RADIOLOGUE or ADMIN can be assigned to reports. User role: " + newRadiologue.getRole());
            }
            
            existingReport.setRadiologue(newRadiologue);
        }

        return reportRepository.save(existingReport);
    }

    @Override
    public void deleteReport(Long id) {
        Report report = findById(id);
        
        // Validation : un report validé ne peut être supprimé que par un ADMIN
        if (report.getValidated()) {
            // Note: Cette validation pourrait être ajoutée au niveau du contrôleur
            // en vérifiant le rôle de l'utilisateur connecté
            throw new IllegalArgumentException("Validated report cannot be deleted");
        }
        
        reportRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Report findById(Long id) {
        return reportRepository.findByIdWithRelations(id)
                .orElseThrow(() -> new ReportNotFoundException("Report not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Report> findAll() {
        return reportRepository.findAllWithRelations();
    }

    @Override
    @Transactional(readOnly = true)
    public Report findByExam(Long examId) {
        return reportRepository.findByExamWithRelations(examId)
                .orElseThrow(() -> new ReportNotFoundException("Report not found for exam id: " + examId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Report> findByRadiologue(Long radiologueId) {
        return reportRepository.findByRadiologueWithRelations(radiologueId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Report> findUnvalidatedReports() {
        return reportRepository.findUnvalidatedWithRelations();
    }

    @Override
    public Report validateReport(Long id, Long radiologueId) {
        Report report = findById(id);
        
        // Validation du radiologue qui valide
        User validatingRadiologue = userRepository.findById(radiologueId)
                .orElseThrow(() -> new UserNotFoundException("Radiologue not found with id: " + radiologueId));
        
        // Seul un RADIOLOGUE peut valider un report
        if (validatingRadiologue.getRole() != UserRole.RADIOLOGUE) {
            throw new IllegalArgumentException("Only RADIOLOGUE can validate reports. User role: " + validatingRadiologue.getRole());
        }
        
        // Le radiologue qui valide doit être le même que celui qui a créé le report
        if (!report.getRadiologue().getId().equals(radiologueId)) {
            throw new IllegalArgumentException("Only the report creator can validate the report");
        }
        
        // Validation du report
        report.setValidated(true);
        report.setValidatedAt(LocalDateTime.now());
        
        return reportRepository.save(report);
    }

    private void validateReportData(Report report) {
        // Validation de l'examen
        if (report.getExam() == null) {
            throw new IllegalArgumentException("Exam is required");
        }

        // Validation du radiologue
        if (report.getRadiologue() == null) {
            throw new IllegalArgumentException("Radiologue is required");
        }

        // Validation des champs de contenu
        if (report.getFindings() == null || report.getFindings().trim().isEmpty()) {
            throw new IllegalArgumentException("Findings cannot be null or empty");
        }
        
        if (report.getConclusion() == null || report.getConclusion().trim().isEmpty()) {
            throw new IllegalArgumentException("Conclusion cannot be null or empty");
        }
    }
}
