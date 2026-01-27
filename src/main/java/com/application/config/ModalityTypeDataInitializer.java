package com.application.config;

import com.application.entity.ModalityType;
import com.application.repository.ModalityTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ModalityTypeDataInitializer implements CommandLineRunner {

    @Autowired
    private ModalityTypeRepository modalityRepo;

    @Override
    public void run(String... args) throws Exception {
        // Vérifier si des modalités existent déjà
        if (modalityRepo.count() > 0) {
            System.out.println("📋 Les modalités existent déjà dans la base de données");
            return;
        }

        System.out.println("📋 Initialisation des types de modalité...");

        // Scanner CT
        createModality("CT", "Scanner", "Scanner tomodensitométrique", "CT", 1);
        
        // IRM
        createModality("MR", "IRM", "Imagerie par résonance magnétique", "MR", 2);
        
        // Échographie
        createModality("US", "Échographie", "Ultrasonographie", "US", 3);
        
        // Radiographie conventionnelle
        createModality("CR", "Radiographie Numérique", "Radiographie numérique", "CR", 4);
        createModality("DX", "Radiographie", "Radiographie conventionnelle", "DX", 5);
        
        // Mammographie
        createModality("MG", "Mammographie", "Mammographie numérique", "MG", 6);
        
        // Médecine nucléaire
        createModality("NM", "Médecine Nucléaire", "Scintigraphie et PET", "NM", 7);
        createModality("PT", "PET", "Tomographie par émission de positons", "PT", 8);
        
        // Fluoroscopie
        createModality("RF", "Fluoroscopie", "Radioscopie", "RF", 9);
        
        // Angiographie
        createModality("XA", "Angiographie", "Angiographie soustractive", "XA", 10);
        
        // Radiothérapie
        createModality("RT", "Radiothérapie", "Radiothérapie externe", "RT", 11);

        System.out.println("✅ " + modalityRepo.count() + " types de modalité ont été initialisés avec succès");
    }

    private void createModality(String code, String name, String description, String dicomCode, int sortOrder) {
        ModalityType modality = new ModalityType();
        modality.setCode(code);
        modality.setName(name);
        modality.setDescription(description);
        modality.setDicomCode(dicomCode);
        modality.setSortOrder(sortOrder);
        modality.setIsActive(true);
        
        modalityRepo.save(modality);
    }
}
