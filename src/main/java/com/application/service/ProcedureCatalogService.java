package com.application.service;

import com.application.entity.ModalityType;
import com.application.entity.ProcedureCatalog;
import com.application.repository.ModalityTypeRepository;
import com.application.repository.ProcedureCatalogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProcedureCatalogService {

    @Autowired
    private ProcedureCatalogRepository procedureRepo;

    @Autowired
    private ModalityTypeRepository modalityRepo;

    public List<ProcedureCatalog> getAllActiveProcedures() {
        return procedureRepo.findAllWithModality();
    }

    public List<ProcedureCatalog> getProceduresByModality(String modalityCode) {
        Optional<ModalityType> modality = modalityRepo.findByCodeAndIsActive(modalityCode, true);
        if (modality.isPresent()) {
            return procedureRepo.findByModalityTypeAndIsActive(modality.get(), true);
        }
        return List.of();
    }

    public Optional<ProcedureCatalog> getProcedureById(Long id) {
        return procedureRepo.findById(id);
    }

    public Optional<ProcedureCatalog> getProcedureByName(String name) {
        return procedureRepo.findByNameAndIsActive(name, true);
    }

    public List<ProcedureCatalog> searchProcedures(String name, String modalityCode, String region) {
        return procedureRepo.searchProcedures(name, modalityCode, region);
    }

    public ProcedureCatalog createProcedure(ProcedureCatalog procedure) {
        procedure.setIsActive(true);
        return procedureRepo.save(procedure);
    }

    public ProcedureCatalog updateProcedure(ProcedureCatalog procedure) {
        return procedureRepo.save(procedure);
    }

    public void deleteProcedure(Long id) {
        Optional<ProcedureCatalog> procedure = procedureRepo.findById(id);
        if (procedure.isPresent()) {
            procedure.get().setIsActive(false);
            procedureRepo.save(procedure.get());
        }
    }

    public List<String> getAllModalityCodes() {
        return procedureRepo.findDistinctModalityCodes();
    }

    public List<ModalityType> getAllActiveModalities() {
        return modalityRepo.findAllActiveOrdered();
    }

    public List<String> getAllRegions() {
        return procedureRepo.findDistinctRegions();
    }

    public ProcedureCatalog createOrUpdateProcedureFromData(String name, String modalityCode, String region, 
                                                          String laterality, Boolean contrastRequired,
                                                          String contrastType, String injectionRate, 
                                                          String contrastVolume, String description) {
        // Trouver la modalité
        Optional<ModalityType> modalityOpt = modalityRepo.findByCodeAndIsActive(modalityCode, true);
        if (modalityOpt.isEmpty()) {
            throw new IllegalArgumentException("Modalité non trouvée: " + modalityCode);
        }
        
        // Vérifier si une procédure similaire existe déjà
        List<ProcedureCatalog> existing = searchProcedures(name, modalityCode, region);
        
        if (!existing.isEmpty()) {
            // Retourner la première procédure existante trouvée
            return existing.get(0);
        }
        
        // Créer une nouvelle procédure
        ProcedureCatalog newProcedure = new ProcedureCatalog();
        newProcedure.setName(name);
        newProcedure.setModalityType(modalityOpt.get());
        newProcedure.setRegion(region);
        newProcedure.setLaterality(laterality);
        newProcedure.setContrastRequired(contrastRequired != null ? contrastRequired : false);
        newProcedure.setContrastType(contrastType);
        newProcedure.setInjectionRate(injectionRate);
        newProcedure.setContrastVolume(contrastVolume);
        newProcedure.setDescription(description);
        newProcedure.setIsActive(true);
        
        return procedureRepo.save(newProcedure);
    }
}
