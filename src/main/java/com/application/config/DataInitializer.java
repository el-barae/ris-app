package com.application.config;

import com.application.entity.*;
import com.application.service.*;
import com.application.repository.ProcedureCatalogRepository;
import com.application.repository.ModalityTypeRepository;
import com.application.repository.ModalityRepository;
import com.application.repository.HospitalRepository;
import com.application.repository.ProcedureRepository;
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

    @Autowired
    private ModalityTypeRepository modalityTypeRepository;

    @Autowired
    private ModalityRepository modalityRepository;

    @Autowired
    private ProcedureCatalogRepository procedureCatalogRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private ProcedureRepository procedureRepository;

    @Autowired
    private OrderService orderService;

    private final Random random = new Random();

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== Démarrage de l'initialisation des données ===");
        
        // Initialiser dans le bon ordre pour respecter les dépendances
        initializeHospitals();
        initializeModalityTypes();
        initializeModalities();
        initializeProcedures();
        initializeUsers();
        initializePatients();
        initializeOrders();
        initializeProceduresForExams();
        initializeExams();
        // initializeReports(); // Désactivé pour éviter les erreurs d'initialisation
        
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
        createUser("admin", "admin123", "Admin", "System", UserRole.ADMIN, "admin@radiology.com");
        createUser("dr_dupont", "admin123", "Jean", "Dupont", UserRole.MEDECIN, "dupont@radiology.com");
        createUser("dr_martin", "admin123", "Marie", "Martin", UserRole.MEDECIN, "martin@radiology.com");
        createUser("tech1", "admin123", "Pierre", "Technicien", UserRole.TECHNICIEN, "tech1@radiology.com");
        createUser("sec1", "admin123", "Sophie", "Secrétaire", UserRole.SECRETAIRE, "sec1@radiology.com");
        createUser("radio1", "admin123", "Robert", "Radiologue", UserRole.RADIOLOGUE, "radio1@radiology.com");

        System.out.println("✅ Utilisateurs initialisés");
    }

    private void createUser(String username, String password, String firstName, String lastName, UserRole role, String email) {
        try {
            System.out.println("Tentative de création de l'utilisateur: " + username);
            User existingUser = userService.findByUsername(username);
            System.out.println("L'utilisateur " + username + " existe déjà");
            
            // Vérifier si l'utilisateur a un hôpital, sinon le mettre à jour
            if (existingUser.getHospital() == null) {
                List<Hospital> hospitals = hospitalRepository.findAll();
                if (!hospitals.isEmpty()) {
                    existingUser.setHospital(hospitals.get(0));
                    userService.updateUser(existingUser.getId(), existingUser);
                    System.out.println("  -> Utilisateur mis à jour et rattaché à l'hôpital: " + hospitals.get(0).getName());
                }
            }
        } catch (Exception e) {
            System.out.println("Création de l'utilisateur: " + username);
            User user = new User();
            user.setUsername(username);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setRole(role);
            user.setEmail(email);
            user.setActive(true);
            
            // Rattacher l'utilisateur à l'hôpital par défaut
            List<Hospital> hospitals = hospitalRepository.findAll();
            if (!hospitals.isEmpty()) {
                user.setHospital(hospitals.get(0));
                System.out.println("  -> Rattaché à l'hôpital: " + hospitals.get(0).getName());
            }
            
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

    private void initializeModalityTypes() {
        System.out.println("=== Initialisation des types de modalités ===");
        
        if (modalityTypeRepository.count() > 0) {
            System.out.println("=== Les types de modalités existent déjà ===");
            return;
        }

        // Créer les types de modalités
        createModalityType("CT", "Tomodensitométrie", "CT");
        createModalityType("MRI", "Imagerie par Résonance Magnétique", "MR");
        createModalityType("RX", "Radiographie", "XR");
        createModalityType("US", "Échographie", "US");
        createModalityType("MG", "Mammographie", "MG");
        createModalityType("RF", "Radioscopie", "RF");
        createModalityType("PT", "Tomographie par Émission de Positrons", "PT");
        createModalityType("XA", "Angiographie", "XA");
        createModalityType("CR", "Radiographie Numérisée", "CR");
        createModalityType("DX", "Radiographie Numérique", "DX");

        System.out.println("✅ Types de modalités initialisés");
    }

    private void createModalityType(String code, String name, String dicomCode) {
        ModalityType modalityType = new ModalityType();
        modalityType.setCode(code);
        modalityType.setName(name);
        modalityType.setDicomCode(dicomCode);
        modalityType.setIsActive(true);
        modalityTypeRepository.save(modalityType);
    }

    private void initializeModalities() {
        System.out.println("=== Initialisation des modalités (équipements) ===");
        
        if (modalityRepository.count() > 0) {
            System.out.println("=== Les modalités existent déjà ===");
            return;
        }

        List<ModalityType> modalityTypes = modalityTypeRepository.findAll();
        
        // Équipements CT
        createModality("CT1", "CT Siemens Somatom", "CT 64 tranches", "Siemens", modalityTypes.get(0));
        createModality("CT2", "CT GE Lightspeed", "CT 128 tranches", "GE Healthcare", modalityTypes.get(0));

        // Équipements IRM
        createModality("MRI1", "IRM Siemens Skyra", "IRM 3.0 Tesla", "Siemens", modalityTypes.get(1));
        createModality("MRI2", "IRM GE Signa", "IRM 1.5 Tesla", "GE Healthcare", modalityTypes.get(1));

        // Équipements Radiographie
        createModality("RX1", "Radiographie Fixe", "Salle de radiographie conventionnelle", "Philips", modalityTypes.get(2));
        createModality("RX2", "Radiographie Mobile", "Radiographie mobile au lit", "Siemens", modalityTypes.get(2));

        // Équipements Échographie
        createModality("US1", "Échographie Voluson", "Échographie obstétrique", "GE Healthcare", modalityTypes.get(3));
        createModality("US2", "Échographie Philips", "Échographie générale", "Philips", modalityTypes.get(3));

        // Équipements Mammographie
        createModality("MG1", "Mammographie Hologic", "Mammographie numérique", "Hologic", modalityTypes.get(4));

        // Équipements Radioscopie
        createModality("RF1", "Radioscopie Siemens", "Salle de radioscopie", "Siemens", modalityTypes.get(5));

        System.out.println("✅ Modalités initialisées");
    }

    private void createModality(String aetitle, String nom, String description, String marque, ModalityType modalityType) {
        Modality modality = new Modality();
        modality.setAetitle(aetitle);
        modality.setNom(nom);
        modality.setDescription(description);
        modality.setMarque(marque);
        modality.setIsActive(true);
        modality.setModalityType(modalityType);
        modalityRepository.save(modality);
    }

    private void initializeProcedures() {
        System.out.println("=== Initialisation des procédures ===");
        
        if (procedureRepository.count() > 0) {
            System.out.println("=== Les procédures existent déjà ===");
            return;
        }

        List<ModalityType> modalityTypes = modalityTypeRepository.findAll();
        
        // Procédures CT
        createProcedure("CT-CHEST", "CT Thorax", "Scanner du thorax avec et sans contraste", "Chest", true, "Iodé", 15, modalityTypes.get(0));
        createProcedure("CT-ABDOMEN", "CT Abdomen", "Scanner de l'abdomen avec contraste", "Abdomen", true, "Iodé", 20, modalityTypes.get(0));
        createProcedure("CT-HEAD", "CT Crâne", "Scanner du crâne sans contraste", "Head", false, null, 10, modalityTypes.get(0));
        createProcedure("CT-SPINE", "CT Rachis", "Scanner du rachis lombaire", "Spine", false, null, 15, modalityTypes.get(0));

        // Procédures IRM
        createProcedure("MRI-BRAIN", "IRM Cerveau", "IRM cérébrale avec et sans contraste", "Head", true, "Gadolinium", 30, modalityTypes.get(1));
        createProcedure("MRI-KNEE", "IRM Genou", "IRM du genou sans contraste", "Extremity", false, null, 25, modalityTypes.get(1));
        createProcedure("MRI-SPINE", "MRI Rachis", "IRM du rachis cervical", "Spine", false, null, 30, modalityTypes.get(1));

        // Procédures Radiographie
        createProcedure("RX-CHEST", "Radio Thorax", "Radiographie pulmonaire de face et profil", "Chest", false, null, 10, modalityTypes.get(2));
        createProcedure("RX-ABDOMEN", "RX Abdomen", "Radiographie abdominale sans préparation", "Abdomen", false, null, 10, modalityTypes.get(2));
        createProcedure("RX-EXTREMITY", "RX Membre", "Radiographie de membre (bras/jambe)", "Extremity", false, null, 10, modalityTypes.get(2));

        // Procédures Échographie
        createProcedure("US-ABDOMEN", "Écho Abdomen", "Échographie abdominale complète", "Abdomen", false, null, 20, modalityTypes.get(3));
        createProcedure("US-PELVIS", "Écho Pelvien", "Échographie pelvienne", "Pelvis", false, null, 15, modalityTypes.get(3));
        createProcedure("US-CAROTID", "Écho Carotides", "Échographie des artères carotides", "Neck", false, null, 15, modalityTypes.get(3));
        createProcedure("US-OBSTETRIC", "Écho Obstétricale", "Échographie obstétricale", "Pelvis", false, null, 25, modalityTypes.get(3));

        // Procédures Mammographie
        createProcedure("MG-SCREENING", "Mammo Dépistage", "Mammographie de dépistage bilatérale", "Chest", false, null, 15, modalityTypes.get(4));
        createProcedure("MG-DIAGNOSTIC", "Mammo Diagnostic", "Mammographie diagnostique unilatérale", "Chest", false, null, 20, modalityTypes.get(4));

        // Procédures Radioscopie
        createProcedure("RF-GI", "Scopie Digestive", "Transit œso-gastro-duodénal", "Abdomen", true, "Baryum", 30, modalityTypes.get(5));

        System.out.println("✅ Procédures initialisées");
    }

    private void initializeHospitals() {
        System.out.println("=== Initialisation des hôpitaux ===");
        
        if (hospitalRepository.count() > 0) {
            System.out.println("=== Les hôpitaux existent déjà ===");
            return;
        }

        // Créer un hôpital par défaut
        Hospital hospital = new Hospital();
        hospital.setName("Hôpital Central");
        hospital.setAddress("123 Rue de la Santé");
        hospital.setCity("Paris");
        hospital.setPostalCode("75014");
        hospital.setPhone("01 23 45 67 89");
        hospital.setEmail("contact@hopital-central.fr");
        hospital.setIsActive(true);
        
        hospitalRepository.save(hospital);
        System.out.println("✅ Hôpitaux initialisés");
    }

    private void createProcedure(String procedureCode, String name, String description, String region, boolean contrastRequired, String contrastType, int duration, ModalityType modalityType) {
        ProcedureCatalog procedure = new ProcedureCatalog();
        procedure.setProcedureCode(procedureCode);
        procedure.setName(name);
        procedure.setDescription(description);
        procedure.setRegion(region);
        procedure.setContrastRequired(contrastRequired);
        procedure.setContrastType(contrastType);
        procedure.setIsActive(true);
        procedure.setModalityType(modalityType);
        procedureCatalogRepository.save(procedure);
    }

    private void initializeOrders() {
        System.out.println("=== Initialisation des ordres ===");
        
        if (orderService.findAll().size() >= 7) {
            System.out.println("=== Les ordres existent déjà ===");
            return;
        }

        List<Patient> patients = patientService.findAll();
        List<User> medecins = userService.findByRole(UserRole.MEDECIN);
        List<Hospital> hospitals = hospitalRepository.findAll();

        if (patients.isEmpty() || medecins.isEmpty() || hospitals.isEmpty()) {
            System.err.println("❌ Données manquantes pour créer les ordres");
            return;
        }

        // Créer 7 ordres pour les examens
        for (int i = 0; i < 7; i++) {
            Order order = new Order();
            order.setStudyInstanceUID(generateStudyInstanceUID());
            order.setAccessionNumber(generateAccessionNumber());
            order.setPatient(patients.get(i % patients.size()));
            order.setDoctor(medecins.get(i % medecins.size()));
            order.setHospital(hospitals.get(0));
            
            try {
                orderService.createOrder(order);
                System.out.println("✅ Ordre créé: " + order.getAccessionNumber());
            } catch (Exception e) {
                System.err.println("❌ Erreur création ordre: " + e.getMessage());
            }
        }

        System.out.println("✅ Ordres initialisés");
    }

    private void initializeProceduresForExams() {
        System.out.println("=== Initialisation des procédures spécifiques aux examens ===");
        
        if (procedureRepository.count() >= 7) {
            System.out.println("=== Les procédures spécifiques existent déjà ===");
            return;
        }

        List<ProcedureCatalog> procedureCatalogs = procedureCatalogRepository.findAll();
        
        // Créer une procédure spécifique pour chaque examen (7 examens)
        for (int i = 0; i < 7; i++) {
            ProcedureCatalog catalog = procedureCatalogs.get(i % procedureCatalogs.size());
            Procedure procedure = new Procedure();
            
            // Copier les attributs du catalogue
            procedure.setName(catalog.getName());
            procedure.setProcedureCode(catalog.getProcedureCode());
            procedure.setModalityType(catalog.getModalityType());
            procedure.setRegion(catalog.getRegion());
            procedure.setLaterality(catalog.getLaterality());
            procedure.setContrastRequired(catalog.getContrastRequired());
            procedure.setContrastType(catalog.getContrastType());
            procedure.setInjectionRate(catalog.getInjectionRate() != null ? Double.parseDouble(catalog.getInjectionRate()) : null);
            procedure.setInjectionVolume(catalog.getContrastVolume() != null ? Double.parseDouble(catalog.getContrastVolume()) : null);
            procedure.setDescription(catalog.getDescription());
            procedure.setSpecialInstructions(catalog.getAdditionalInstructions());
            procedure.setIsActive(true);
            procedure.setIsEmergency(false);
            procedure.setScheduledDurationMinutes(30); // Valeur par défaut
            procedure.setNotes("Procédure créée pour l'examen " + (i + 1));
            
            // Lier au catalogue (template)
            procedure.setProcedureCatalog(catalog);
            
            try {
                procedureRepository.save(procedure);
                System.out.println("✅ Procédure spécifique créée: " + procedure.getProcedureCode());
            } catch (Exception e) {
                System.err.println("❌ Erreur création procédure spécifique: " + e.getMessage());
            }
        }

        System.out.println("✅ Procédures spécifiques initialisées");
    }

    private String generateStudyInstanceUID() {
        return "1.2.840.113619.2.55.3.604688237.761.1243134237." + System.currentTimeMillis();
    }

    private String generateAccessionNumber() {
        return "ACC" + System.currentTimeMillis();
    }
    private void initializeExams() {
        System.out.println("=== Initialisation des examens ===");
        
        // Vérifier si les examens existent déjà
        List<Exam> existingExams = examService.findAll();
        if (existingExams.size() >= 7) {
            System.out.println("=== Les examens existent déjà ===");
            return;
        }

        List<Order> orders = orderService.findAll();
        List<ModalityType> modalityTypes = modalityTypeRepository.findAllActiveOrdered();
        List<Modality> modalities = modalityRepository.findByIsActive(true);
        List<Procedure> procedures = procedureRepository.findAll();

        if (orders.isEmpty() || modalityTypes.isEmpty()) {
            System.err.println("❌ Données manquantes pour créer les examens");
            return;
        }

        // Créer 5 examens PLANNED pour le worklist gauche
        for (int i = 0; i < 5; i++) {
            Exam exam = new Exam();
            exam.setOrder(orders.get(i % orders.size()));
            exam.setModalityType(modalityTypes.get(i % modalityTypes.size()));
            exam.setModalityEntity(modalities.get(i % modalities.size()));
            exam.setProcedure(procedures.get(i % procedures.size()));
            exam.setStatus(ExamStatus.PLANNED);
            exam.setPriority(Priority.NORMAL);
            exam.setScheduledDateTime(LocalDateTime.now().plusDays(i).plusHours(9 + (i * 2)));
            exam.setAdditionalInstructions("Examen planifié " + (i + 1) + " - Instructions standards");

            try {
                examService.createExam(exam);
                System.out.println("✅ Examen PLANNED créé: " + exam.getAccessionNumber());
            } catch (Exception e) {
                System.err.println("❌ Erreur création examen PLANNED: " + e.getMessage());
            }
        }

        // Créer 2 examens SELECTED pour le worklist droite
        for (int i = 0; i < 2; i++) {
            Exam exam = new Exam();
            exam.setOrder(orders.get((i + 1) % orders.size()));
            exam.setModalityType(modalityTypes.get((i + 2) % modalityTypes.size()));
            exam.setModalityEntity(modalities.get((i + 2) % modalities.size()));
            exam.setProcedure(procedures.get((i + 3) % procedures.size()));
            exam.setStatus(ExamStatus.SELECTED);
            exam.setPriority(i == 0 ? Priority.URGENT : Priority.NORMAL);
            exam.setScheduledDateTime(LocalDateTime.now().plusDays(1).plusHours(10 + (i * 3)));
            exam.setAdditionalInstructions("Examen sélectionné " + (i + 1) + " - Prêt pour MWL");

            try {
                examService.createExam(exam);
                System.out.println("✅ Examen SELECTED créé: " + exam.getAccessionNumber());
            } catch (Exception e) {
                System.err.println("❌ Erreur création examen SELECTED: " + e.getMessage());
            }
        }

        System.out.println("✅ Examens initialisés");
    }

    private void initializeReports() {
        System.out.println("=== Initialisation des rapports ===");
        
        // Vérifier si les rapports existent déjà
        List<Report> existingReports = reportService.findAll();
        if (existingReports.size() >= 2) {
            System.out.println("=== Les rapports existent déjà ===");
            return;
        }

        List<Exam> completedExams = examService.findByStatus(ExamStatus.COMPLETED);
        List<User> radiologues = userService.findByRole(UserRole.RADIOLOGUE);

        if (radiologues.isEmpty()) {
            System.err.println("❌ Aucun radiologue trouvé - Impossible de créer des rapports");
            return;
        }

        if (completedExams.isEmpty()) {
            System.out.println("=== Aucun examen complété trouvé - Création d'examens complétés pour les rapports ===");
            // Créer quelques examens complétés si aucun n'existe
            List<Patient> patients = patientService.findAll();
            List<ModalityType> modalityTypes = modalityTypeRepository.findAllActiveOrdered();
            List<User> doctors = userService.findByRole(UserRole.MEDECIN);
            List<Hospital> hospitals = hospitalRepository.findAll();
            List<Procedure> procedures = procedureRepository.findAll();
            
            if (patients.isEmpty() || doctors.isEmpty() || hospitals.isEmpty() || procedures.isEmpty()) {
                System.err.println("❌ Données manquantes pour créer des examens complétés");
                return;
            }
            
            for (int i = 0; i < 2; i++) {
                Exam exam = new Exam();
                // Créer un order pour l'examen
                Order order = new Order();
                order.setPatient(patients.get(i % patients.size()));
                order.setDoctor(doctors.get(i % doctors.size()));
                order.setHospital(hospitals.get(i % hospitals.size()));
                order.setAccessionNumber("ORD-" + System.currentTimeMillis() + i);
                order.setStudyInstanceUID("1.2.840.113619.2.55.3.604688237." + System.currentTimeMillis() + i);
                order.setCreatedAt(LocalDateTime.now().minusDays(1));
                
                try {
                    order = orderService.createOrder(order);
                    exam.setOrder(order);
                    exam.setModalityType(modalityTypes.get(i % modalityTypes.size()));
                    exam.setProcedure(procedures.get(i % procedures.size()));
                    exam.setStatus(ExamStatus.COMPLETED);
                    exam.setPriority(Priority.NORMAL);
                    exam.setScheduledDateTime(LocalDateTime.now().minusDays(1));
                    exam.setPerformedDateTime(LocalDateTime.now().minusDays(1).plusHours(2));
                    exam.setAdditionalInstructions("Examen complété " + (i + 1));
                    
                    examService.createExam(exam);
                    completedExams.add(exam);
                } catch (Exception e) {
                    System.err.println("❌ Erreur création examen complété: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        int reportCount = Math.min(2, Math.min(completedExams.size(), radiologues.size()));
        for (int i = 0; i < reportCount; i++) {
            Report report = new Report();
            report.setExam(completedExams.get(i));
            report.setRadiologue(radiologues.get(i % radiologues.size()));
            report.setFindings("Observations radiologiques pour l'examen " + (i + 1) + ". " +
                    "Aucune anomalie significative détectée. Structures anatomiques normales.");
            report.setConclusion("Conclusion : Résultats normaux. Aucun suivi nécessaire.");
            report.setValidated(false);

            try {
                Report createdReport = reportService.createReport(report);
                // Valider le rapport seulement s'il a été correctement créé
                if (createdReport != null && createdReport.getId() != null) {
                    reportService.validateReport(createdReport.getId(), createdReport.getRadiologue().getId());
                    System.out.println("✅ Rapport créé et validé: " + createdReport.getId());
                }
            } catch (Exception e) {
                System.err.println("❌ Erreur création rapport: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("✅ Rapports initialisés");
    }
}
