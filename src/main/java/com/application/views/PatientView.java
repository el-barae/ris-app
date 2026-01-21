package com.application.views;

import com.application.entity.Patient;
import com.application.entity.UserRole;
import com.application.security.SecurityUtils;
import com.application.service.PatientService;
import com.application.service.PatientDataCleanupService;
import com.application.views.dialog.PatientDialog;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Route(value = "patients", layout = MainLayout.class)
@PageTitle("Gestion des patients")
@AnonymousAllowed
public class PatientView extends VerticalLayout {

    private final PatientService patientService;
    private final PatientDataCleanupService cleanupService;
    private final Grid<Patient> grid;
    private final TextField searchField;
    private List<Patient> allPatients;

    public PatientView(PatientService patientService, PatientDataCleanupService cleanupService) {
        this.patientService = patientService;
        this.cleanupService = cleanupService;
        this.grid = new Grid<>();
        this.searchField = new TextField();
    }

    @PostConstruct
    public void init() {
        System.out.println("DEBUG: PatientView @PostConstruct called");

        // 1. D'abord créer l'interface (grille vide)
        createPatientView();

        // 2. Ensuite exécuter le nettoyage
        try {
            cleanupService.cleanupDuplicatePatients();
        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }

        // 3. Enfin charger les patients (qui va remplir la grille)
        loadPatients();
    }

    private void createPatientView() {
        addClassName("patient-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Header
        add(createHeader());

        // Grid - la configurer mais ne pas encore y mettre de données
        add(createPatientGrid());
    }

    private Component createPatientGrid() {
        grid.setWidthFull();
        grid.setHeight("600px");
        grid.addClassNames("border-radius-m", "striped-rows");

        // NE PAS mettre grid.setItems(List.of()) ici !
        // Laisser la grille vide, elle sera remplie par loadPatients()

        // Colonnes
        grid.addColumn(Patient::getPatientId)
                .setHeader("Patient ID")
                .setSortable(true);

        grid.addColumn(patient -> patient.getFirstName() + " " + patient.getLastName())
                .setHeader("Nom complet")
                .setSortable(true);

        grid.addColumn(patient -> patient.getDateOfBirth().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .setHeader("Date de naissance")
                .setSortable(true);

        grid.addColumn(this::calculateAge)
                .setHeader("Âge")
                .setSortable(true);

        grid.addColumn(Patient::getPhone)
                .setHeader("Téléphone")
                .setSortable(true);

        grid.addColumn(new ComponentRenderer<>(this::createActionButtons))
                .setHeader("Actions")
                .setSortable(false); // Les actions ne sont généralement pas triables

        return grid;
    }

    private void loadPatients() {
        try {
            allPatients = patientService.findAll();
            System.out.println("DEBUG: Total patients loaded: " + allPatients.size());
            
            // Check for duplicates in the list itself
            Set<String> patientIds = new HashSet<>();
            Set<Long> dbIds = new HashSet<>();
            int duplicatePatientIds = 0;
            int duplicateDbIds = 0;
            
            for (Patient patient : allPatients) {
                if (!patientIds.add(patient.getPatientId())) {
                    duplicatePatientIds++;
                }
                if (!dbIds.add(patient.getId())) {
                    duplicateDbIds++;
                }
            }
            
            System.out.println("DEBUG: Duplicate patient IDs in list: " + duplicatePatientIds);
            System.out.println("DEBUG: Duplicate DB IDs in list: " + duplicateDbIds);
            
            // Check for duplicates in database
            List<Object[]> duplicates = patientService.findDuplicatePatientIds();
            if (!duplicates.isEmpty()) {
                System.out.println("DEBUG: Found duplicate patient IDs in database:");
                duplicates.forEach(dup -> System.out.println("  - PatientID: " + dup[0] + ", Count: " + dup[1]));
            } else {
                System.out.println("DEBUG: No duplicate patient IDs found in database");
            }
            
            allPatients.forEach(p -> System.out.println("DEBUG: Patient - ID: " + p.getPatientId() + ", Name: " + p.getFullName() + ", DB ID: " + p.getId()));
            
            System.out.println("DEBUG: Setting " + allPatients.size() + " patients to grid");
            
            // Set the patients to grid
            grid.setItems(allPatients);
            
        } catch (Exception e) {
            System.err.println("DEBUG: Error loading patients: " + e.getMessage());
            e.printStackTrace();
            // En cas d'erreur, initialiser avec une liste vide
            allPatients = List.of();
            grid.setItems(allPatients);
        }
    }

    private Component createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.addClassNames("view-header", "mb-m", "gap-m");

        // Titre
        H2 title = new H2();
        title.add(VaadinIcon.USERS.create());
        title.add(" Gestion des patients");
        title.addClassNames("mb-0");

        // Champ de recherche
        searchField.setPlaceholder("Rechercher un patient...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setWidth("300px");
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setValueChangeTimeout(300);
        searchField.addValueChangeListener(e -> filterPatients(e.getValue()));

        header.add(title, searchField);
        if (canCreate()) {
            Button newPatientBtn = new Button("Nouveau patient", VaadinIcon.PLUS.create());
            newPatientBtn.addClassNames("primary");
            newPatientBtn.addClickListener(e -> {
                openCreateDialog();
            });
            header.add(newPatientBtn);
        }
        header.setFlexGrow(1, searchField);

        return header;
    }

    private String calculateAge(Patient patient) {
        if (patient.getDateOfBirth() == null) {
            return "N/A";
        }
        return String.valueOf(Period.between(patient.getDateOfBirth(), LocalDate.now()).getYears());
    }

    private Component createActionButtons(Patient patient) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(false);
        actions.setAlignItems(Alignment.CENTER);

        // Bouton modifier
        if (canModify()) {
            Button editBtn = new Button(VaadinIcon.EDIT.create());
            editBtn.addClassNames("small", "icon-button", "tertiary");
            editBtn.getElement().setProperty("title", "Modifier");
            editBtn.addClickListener(e -> {
                openEditDialog(patient);
            });
            actions.add(editBtn);
        }

        // Bouton supprimer
        if (canDelete()) {
            Button deleteBtn = new Button(VaadinIcon.TRASH.create());
            deleteBtn.addClassNames("small", "icon-button", "error");
            deleteBtn.getElement().setProperty("title", "Supprimer");
            deleteBtn.addClickListener(e -> {
                deletePatient(patient);
            });
            actions.add(deleteBtn);
        }

        return actions;
    }

    private void filterPatients(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            System.out.println("DEBUG: No search term, showing all patients: " + allPatients.size());
            grid.setItems(allPatients);
        } else {
            List<Patient> filtered = allPatients.stream()
                    .filter(patient -> 
                        patient.getFirstName().toLowerCase().contains(searchTerm.toLowerCase()) ||
                        patient.getLastName().toLowerCase().contains(searchTerm.toLowerCase()) ||
                        patient.getPatientId().toLowerCase().contains(searchTerm.toLowerCase()) ||
                        (patient.getPhone() != null && patient.getPhone().contains(searchTerm)))
                    .toList();
            System.out.println("DEBUG: Filtered patients for term '" + searchTerm + "': " + filtered.size());
            grid.setItems(filtered);
        }
    }

    // Méthodes de permission
    private boolean canCreate() {
        return true; // Temporaire pour test - SecurityUtils.hasAnyRole(UserRole.ADMIN, UserRole.MEDECIN, UserRole.SECRETAIRE);
    }

    private boolean canModify() {
        return true; // Temporaire pour test - SecurityUtils.hasAnyRole(UserRole.ADMIN, UserRole.SECRETAIRE, UserRole.MEDECIN);
    }

    private boolean canDelete() {
        return true; // Temporaire pour test - SecurityUtils.hasAnyRole(UserRole.ADMIN, UserRole.SECRETAIRE);
    }

    private void showNotification(String message) {
        com.vaadin.flow.component.notification.Notification.show(message, 3000, 
                com.vaadin.flow.component.notification.Notification.Position.TOP_CENTER);
    }

                            
    // ==================== MÉTHODES DE GESTION ====================
    
    public void openCreateDialog() {
        if (canCreate()) {
            PatientDialog dialog = new PatientDialog(null, this::savePatient);
            dialog.open();
        } else {
            showNotification("Vous n'avez pas les permissions pour créer un patient");
        }
    }

    public void openEditDialog(Patient patient) {
        if (canModify()) {
            PatientDialog dialog = new PatientDialog(patient, this::savePatient);
            dialog.open();
        } else {
            showNotification("Vous n'avez pas les permissions pour modifier un patient");
        }
    }

    public void savePatient(Patient patient) {
        try {
            if (patient.getId() == null) {
                patientService.createPatient(patient);
                showNotification("Patient créé avec succès");
            } else {
                patientService.updatePatient(patient.getId(), patient);
                showNotification("Patient modifié avec succès");
            }
            loadPatients(); // Rafraîchir la grille
        } catch (Exception e) {
            showNotification("Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }

    public void deletePatient(Patient patient) {
        if (!canDelete()) {
            showNotification("Vous n'avez pas les permissions pour supprimer un patient");
            return;
        }
        
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("Confirmation de suppression");
        confirmDialog.setText("Êtes-vous sûr de vouloir supprimer le patient " + patient.getFullName() + " ?");
        confirmDialog.setConfirmText("Supprimer");
        confirmDialog.setCancelText("Annuler");
        confirmDialog.setConfirmButtonTheme("error primary");
        confirmDialog.addConfirmListener(e -> {
            try {
                patientService.deletePatient(patient.getId());
                showNotification("Patient supprimé avec succès");
                loadPatients(); // Rafraîchir la grille
            } catch (Exception ex) {
                showNotification("Erreur lors de la suppression: " + ex.getMessage());
            }
        });
        confirmDialog.open();
    }
}
