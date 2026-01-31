package com.application.config;

import com.application.entity.ModalityType;
import com.application.entity.ProcedureCatalog;
import com.application.repository.ModalityTypeRepository;
import com.application.repository.ProcedureCatalogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

//@Component
public class ProcedureDataInitializer implements CommandLineRunner {

    @Autowired
    private ProcedureCatalogRepository procedureRepo;
    
    @Autowired
    private ModalityTypeRepository modalityRepo;

    @Override
    public void run(String... args) throws Exception {
        // Vérifier si des procédures existent déjà
        if (procedureRepo.count() > 0) {
            System.out.println("📋 Les procédures existent déjà dans la base de données");
            return;
        }

        System.out.println("📋 Initialisation des procédures par défaut...");

        // Procédures CT
        createProcedure("CT Abdomen Embolie", "CT", "Abdomen", null, true, "Iohexol 350", "4", "100", "Scanner abdominal avec injection pour recherche d'embolie pulmonaire");
        createProcedure("CT Abdomen Rachis", "CT", "Abdomen", null, false, null, null, null, "Scanner abdominal et rachis lombaire sans injection");
        createProcedure("CT Thorax Standard", "CT", "Chest", null, false, null, null, null, "Scanner thoracique standard sans injection");
        createProcedure("CT Cérébral", "CT", "Head", null, false, null, null, null, "Scanner cérébral sans injection");
        createProcedure("CT Angio Cérébral", "CT", "Head", null, true, "Iohexol 350", "5", "80", "Scanner cérébral avec injection angiographique");
        createProcedure("CT Thoraco-Abdomino-Pelvien", "CT", "Chest", null, true, "Iohexol 350", "3", "120", "Scanner TAP avec injection de contraste");

        // Procédures IRM
        createProcedure("IRM Cérébrale", "MR", "Head", null, false, null, null, null, "IRM cérébrale standard");
        createProcedure("IRM Médullaire", "MR", "Spine", null, false, null, null, null, "IRM du rachis complet");
        createProcedure("IRM Articulaire Genou", "MR", "Extremity", "Droite", false, null, null, null, "IRM du genou droit");
        createProcedure("IRM Articulaire Genou", "MR", "Extremity", "Gauche", false, null, null, null, "IRM du genou gauche");
        createProcedure("IRM Pelvienne", "MR", "Pelvis", null, false, null, null, null, "IRM pelvienne standard");
        createProcedure("IRM Hépatique", "MR", "Abdomen", null, true, "Gadolinium", "2", "15", "IRM hépatique avec injection de chélates de gadolinium");

        // Procédures Radio
        createProcedure("Radio Thorax", "CR", "Chest", null, false, null, null, null, "Radio thoracique face et profil");
        createProcedure("Radio Abdomen", "CR", "Abdomen", null, false, null, null, null, "Radio abdomen sans préparation");
        createProcedure("Radio Rachis Lombaire", "CR", "Spine", null, false, null, null, null, "Radio rachis lombaire face et profil");
        createProcedure("Radio Membres Supérieurs", "CR", "Extremity", null, false, null, null, null, "Radio des membres supérieurs");
        createProcedure("Radio Bassin", "CR", "Pelvis", null, false, null, null, null, "Radio du bassin face");

        // Procédures Échographie
        createProcedure("Échographie Abdominale", "US", "Abdomen", null, false, null, null, null, "Échographie abdominale complète");
        createProcedure("Échographie Pelvienne", "US", "Pelvis", null, false, null, null, null, "Échographie pelvienne sus-pubienne");
        createProcedure("Échographie Cardiaque", "US", "Chest", null, false, null, null, null, "Échographie cardiaque transthoracique");
        createProcedure("Échographie Vasculaire", "US", "Extremity", null, false, null, null, null, "Échographie doppler vasculaire");
        createProcedure("Échographie Thyroïde", "US", "Neck", null, false, null, null, null, "Échographie cervicale thyroïdienne");

        // Procédures Mammographie
        createProcedure("Mammographie Bilatérale", "MG", "Chest", "Bilatéral", false, null, null, null, "Mammographie de dépistage bilatérale");
        createProcedure("Mammographie Unilatérale", "MG", "Chest", "Droite", false, null, null, null, "Mammographie du sein droit");
        createProcedure("Mammographie Unilatérale", "MG", "Chest", "Gauche", false, null, null, null, "Mammographie du sein gauche");

        System.out.println("✅ " + procedureRepo.count() + " procédures ont été initialisées avec succès");
    }

    private void createProcedure(String name, String modalityCode, String region, String laterality, 
                               Boolean contrastRequired, String contrastType, String injectionRate, 
                               String contrastVolume, String description) {
        // Trouver la modalité correspondante
        ModalityType modality = modalityRepo.findByCode(modalityCode)
                .orElseThrow(() -> new IllegalArgumentException("Modalité non trouvée: " + modalityCode));
        
        ProcedureCatalog procedure = new ProcedureCatalog();
        procedure.setName(name);
        procedure.setModalityType(modality);
        procedure.setRegion(region);
        procedure.setLaterality(laterality);
        procedure.setContrastRequired(contrastRequired);
        procedure.setContrastType(contrastType);
        procedure.setInjectionRate(injectionRate);
        procedure.setContrastVolume(contrastVolume);
        procedure.setDescription(description);
        procedure.setAdditionalInstructions("Procédure standardisée");
        procedure.setIsActive(true);
        
        procedureRepo.save(procedure);
    }
}
