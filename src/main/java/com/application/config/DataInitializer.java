package com.application.config;

import com.application.entity.*;
import com.application.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private ExamService examService;

    @Autowired
    private ReportService reportService;

    private final Random random = new Random();

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== Démarrage de l'initialisation des données ===");
        initializeUsers();
        initializePatients();
        initializeExams();
        initializeReports();
        System.out.println("=== Initialisation des données terminée ===");
    }

    private void initializeUsers() {
        System.out.println("=== Initialisation des utilisateurs ===");
        
        // Vérifier si les utilisateurs existent déjà
        try {
            userService.findByUsername("admin");
            System.out.println("=== Les utilisateurs existent déjà ===");
            return; // Les données existent déjà
        } catch (Exception e) {
            System.out.println("=== Création des utilisateurs ===");
            // Les données n'existent pas, on les crée
        }

        // Créer les utilisateurs
        createUser("admin", "admin123", "Admin", "User", UserRole.ADMIN, "admin@hospital.com");
        createUser("medecin", "medecin123", "Jean", "Dupont", UserRole.MEDECIN, "j.dupont@hospital.com");
        createUser("technicien", "tech123", "Marie", "Martin", UserRole.TECHNICIEN, "m.martin@hospital.com");
        createUser("radiologue", "radio123", "Pierre", "Durand", UserRole.RADIOLOGUE, "p.durand@hospital.com");
        createUser("secretaire", "secret123", "Sophie", "Lefebvre", UserRole.SECRETAIRE, "s.lefebvre@hospital.com");

        System.out.println("✅ Utilisateurs initialisés");
    }

    private void createUser(String username, String password, String firstName, String lastName, UserRole role, String email) {
        try {
            System.out.println("Tentative de création de l'utilisateur: " + username);
            userService.findByUsername(username);
            System.out.println("L'utilisateur " + username + " existe déjà");
        } catch (Exception e) {
            System.out.println("Création de l'utilisateur: " + username);
            User user = new User();
            user.setUsername(username);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setRole(role);
            user.setEmail(email);
            user.setActive(true);
            try {
                userService.createUser(user, password);
                System.out.println("✅ Utilisateur créé: " + username);
            } catch (Exception ex) {
                System.err.println("❌ Erreur lors de la création de l'utilisateur " + username + ": " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    private void initializePatients() {
        System.out.println("=== Initialisation des patients ===");
        
        // Clean up any existing Gender.OTHER values first
        try {
            int updatedCount = patientService.cleanupGenderOtherValues();
            if (updatedCount > 0) {
                System.out.println("=== Nettoyage de " + updatedCount + " patients avec Gender.OTHER vers MALE ===");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du nettoyage des Gender.OTHER: " + e.getMessage());
        }
        
        // Vérifier si les patients existent déjà
        try {
            List<Patient> existingPatients = patientService.findAll();
            if (existingPatients.size() >= 10) {
                System.out.println("=== Les patients existent déjà ===");
                return;
            }
        } catch (Exception e) {
            // Handle case where existing patients have Gender.OTHER values
            if (e.getMessage() != null && e.getMessage().contains("No enum constant com.application.entity.Gender.OTHER")) {
                System.out.println("=== Nettoyage des patients avec Gender.OTHER ===");
                // This will be handled by the database migration
                // For now, we'll continue with initialization
            } else {
                System.err.println("Erreur lors de la vérification des patients: " + e.getMessage());
                return;
            }
        }

        String[] firstNames = {"Pierre", "Marie", "Jean", "Sophie", "Michel", "Isabelle", "Philippe", "Nathalie", "Alain", "Catherine"};
        String[] lastNames = {"Martin", "Bernard", "Dubois", "Thomas", "Robert", "Richard", "Petit", "Durand", "Leroy", "Moreau"};
        Gender[] genders = {Gender.MALE, Gender.FEMALE};

        for (int i = 0; i < 10; i++) {
            Patient patient = new Patient();
            patient.setFirstName(firstNames[i]);
            patient.setLastName(lastNames[i]);
            patient.setGender(genders[i % 2]);
            patient.setDateOfBirth(LocalDate.now().minusYears(20 + random.nextInt(60)).minusMonths(random.nextInt(12)).minusDays(random.nextInt(30)));
            patient.setPhone("06" + String.format("%08d", random.nextInt(100000000)));
            patient.setEmail(patient.getFirstName().toLowerCase() + "." + patient.getLastName().toLowerCase() + "@email.com");
            patient.setAddress(random.nextInt(200) + " Rue de la République");
            patient.setCity("Paris");
            patient.setPostalCode("750" + String.format("%02d", random.nextInt(20) + 1));
            
            try {
                patientService.createPatient(patient);
            } catch (Exception e) {
                // Le patient existe déjà
            }
        }

        System.out.println("✅ Patients initialisés");
    }

    private void initializeExams() {
        // Vérifier si les examens existent déjà
        List<Exam> existingExams = examService.findAll();
        if (existingExams.size() >= 15) {
            return;
        }

        List<Patient> patients = patientService.findAll();
        List<User> medecins = userService.findByRole(UserRole.MEDECIN);
        List<ExamType> examTypes = Arrays.asList(ExamType.values());
        List<ExamStatus> statuses = Arrays.asList(ExamStatus.PLANNED, ExamStatus.IN_PROGRESS, ExamStatus.COMPLETED);
        List<Priority> priorities = Arrays.asList(Priority.values());

        for (int i = 0; i < 15; i++) {
            Exam exam = new Exam();
            exam.setPatient(patients.get(random.nextInt(patients.size())));
            exam.setMedecin(medecins.get(random.nextInt(medecins.size())));
            exam.setExamType(examTypes.get(random.nextInt(examTypes.size())));
            exam.setScheduledDateTime(LocalDateTime.now().plusDays(random.nextInt(7)).plusHours(random.nextInt(24)));
            exam.setStatus(statuses.get(random.nextInt(statuses.size())));
            exam.setPriority(priorities.get(random.nextInt(priorities.size())));
            exam.setAdditionalInstructions("Examen " + (i + 1) + " - Instructions standards");

            if (exam.getStatus() == ExamStatus.COMPLETED) {
                exam.setPerformedDateTime(exam.getScheduledDateTime().plusHours(random.nextInt(3)));
            }

            try {
                examService.createExam(exam);
            } catch (Exception e) {
                // L'examen existe déjà
            }
        }

        System.out.println("✅ Examens initialisés");
    }

    private void initializeReports() {
        // Vérifier si les rapports existent déjà
        List<Report> existingReports = reportService.findAll();
        if (existingReports.size() >= 3) {
            return;
        }

        List<Exam> completedExams = examService.findByStatus(ExamStatus.COMPLETED);
        List<User> radiologues = userService.findByRole(UserRole.RADIOLOGUE);

        int reportCount = Math.min(3, completedExams.size());
        for (int i = 0; i < reportCount; i++) {
            Report report = new Report();
            report.setExam(completedExams.get(i));
            report.setRadiologue(radiologues.get(random.nextInt(radiologues.size())));
            report.setFindings("Observations radiologiques pour l'examen " + (i + 1) + ". " +
                    "Aucune anomalie significative détectée. Structures anatomiques normales.");
            report.setConclusion("Conclusion : Résultats normaux. Aucun suivi nécessaire.");

            try {
                reportService.createReport(report);
                // Valider le rapport
                reportService.validateReport(report.getId(), report.getRadiologue().getId());
            } catch (Exception e) {
                // Le rapport existe déjà
            }
        }

        System.out.println("✅ Rapports initialisés");
    }
}
