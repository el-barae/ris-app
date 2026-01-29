package com.application.views;

import com.application.entity.*;
import com.application.service.UserService;
import com.application.service.MWLService;
import com.application.service.ModalityService;
import com.application.service.PacsService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import java.util.Arrays;
import java.util.List;

@Route(value = "settings", layout = MainLayout.class)
@PageTitle("Paramètres")
@RolesAllowed("ADMIN")
public class SettingsView extends VerticalLayout {

    private final UserService userService;
    private final MWLService mwlService;
    private final ModalityService modalityService;
    private final PacsService pacsService;
    private final Grid<User> userGrid;
    private final Grid<ExamType> examTypeGrid;
    private final TabSheet tabSheet;

    // Configuration DICOM fields
    private final TextField risAeTitle;
    private final IntegerField mwlPort;
    private final TextField pacsAeTitle;
    private final TextField pacsIp;
    private final IntegerField pacsPort;

    // Additional DICOM fields
    private final TextField modalityAeTitle;
    private final TextField modalityHost;
    private final IntegerField modalityPort;
    private final Span mwlStatusBadge;

    public SettingsView(UserService userService, MWLService mwlService, ModalityService modalityService, PacsService pacsService) {
        this.userService = userService;
        this.mwlService = mwlService;
        this.modalityService = modalityService;
        this.pacsService = pacsService;
        
        // Initialize components
        this.userGrid = createUserGrid();
        this.examTypeGrid = createExamTypeGrid();
        this.tabSheet = new TabSheet();
        
        // Initialize DICOM fields
        this.risAeTitle = new TextField("AE Title RIS");
        this.mwlPort = new IntegerField("Port MWL");
        this.pacsAeTitle = new TextField("AE Title PACS");
        this.pacsIp = new TextField("IP PACS");
        this.pacsPort = new IntegerField("Port PACS");
        
        // Initialize additional DICOM fields
        this.modalityAeTitle = new TextField("AE Title Modality");
        this.modalityHost = new TextField("Host Modality");
        this.modalityPort = new IntegerField("Port Modality");
        this.mwlStatusBadge = new Span();
        
        // Set default values
        setDefaultValues();
        
        // Create layout
        createLayout();
    }

    private void createLayout() {
        addClassName("settings-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Header
        H2 header = new H2();
        header.add(VaadinIcon.COG.create());
        header.add(" Paramètres système");
        header.addClassNames("view-header", "mb-l", "text-primary");

        // Create tabs
        Tab usersTab = new Tab("Utilisateurs");
        Tab dicomTab = new Tab("Configuration DICOM");
        Tab examTypesTab = new Tab("Types d'examens");

        // Tab content
        VerticalLayout usersContent = createUsersTab();
        VerticalLayout dicomContent = createDicomTab();
        VerticalLayout examTypesContent = createExamTypesTab();

        tabSheet.add(usersTab, usersContent);
        tabSheet.add(dicomTab, dicomContent);
        tabSheet.add(examTypesTab, examTypesContent);
        tabSheet.setWidthFull();

        add(header, tabSheet);
    }

    private VerticalLayout createUsersTab() {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(false);

        // Header with add button
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode.BETWEEN);

        Button addUserBtn = new Button("Ajouter utilisateur", VaadinIcon.PLUS.create());
        addUserBtn.addClassNames("primary");
        addUserBtn.addClickListener(e -> openUserDialog(null));

        headerLayout.add(new H2("Gestion des utilisateurs"), addUserBtn);

        // Grid
        loadUsers();
        // userGrid.setSizeFull(); // Removed to use specific height

        content.add(headerLayout, userGrid);
        content.setFlexGrow(1, userGrid);

        return content;
    }

    private VerticalLayout createDicomTab() {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(false);

        H2 title = new H2("Configuration DICOM");
        title.addClassNames("mb-m");

        // Main form with 2 columns
        FormLayout mainForm = new FormLayout();
        mainForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0px", 2));
        mainForm.setWidthFull();

        // Section 1 - Serveur MWL RIS
        VerticalLayout mwlSection = new VerticalLayout();
        mwlSection.addClassNames("border-radius-m", "p-m", "mb-m", "bg-contrast-5pct");
        
        H3 mwlTitle = new H3("Serveur MWL RIS");
        mwlTitle.addClassNames("mb-s", "text-primary");
        
        FormLayout mwlForm = new FormLayout();
        mwlForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0px", 2));
        mwlForm.add(risAeTitle, mwlPort);
        
        // MWL status badge and restart button
        HorizontalLayout mwlStatusLayout = new HorizontalLayout();
        mwlStatusLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        mwlStatusLayout.setSpacing(true);
        
        updateMWLStatus();
        mwlStatusBadge.addClassNames("badge", "text-s", "font-semibold", "p-xs");
        
        Button restartMWLBtn = new Button("Redémarrer serveur MWL", VaadinIcon.REFRESH.create());
        restartMWLBtn.addClassNames("secondary");
        restartMWLBtn.addClickListener(e -> restartMWLServer());
        
        mwlStatusLayout.add(mwlStatusBadge, restartMWLBtn);
        mwlSection.add(mwlTitle, mwlForm, mwlStatusLayout);

        // Section 2 - Modality
        VerticalLayout modalitySection = new VerticalLayout();
        modalitySection.addClassNames("border-radius-m", "p-m", "mb-m", "bg-contrast-5pct");
        
        H3 modalityTitle = new H3("Modality");
        modalityTitle.addClassNames("mb-s", "text-primary");
        
        FormLayout modalityForm = new FormLayout();
        modalityForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0px", 2));
        modalityForm.add(modalityAeTitle, modalityHost, modalityPort);
        
        Button testModalityBtn = new Button("Tester connexion", VaadinIcon.CONNECT.create());
        testModalityBtn.addClassNames("secondary");
        testModalityBtn.addClickListener(e -> testModalityConnection());
        
        modalitySection.add(modalityTitle, modalityForm, testModalityBtn);

        // Section 3 - PACS
        VerticalLayout pacsSection = new VerticalLayout();
        pacsSection.addClassNames("border-radius-m", "p-m", "mb-m", "bg-contrast-5pct");
        
        H3 pacsTitle = new H3("PACS");
        pacsTitle.addClassNames("mb-s", "text-primary");
        
        FormLayout pacsForm = new FormLayout();
        pacsForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0px", 2));
        pacsForm.add(pacsAeTitle, pacsIp, pacsPort);
        
        Button testPacsBtn = new Button("Tester connexion", VaadinIcon.CONNECT.create());
        testPacsBtn.addClassNames("secondary");
        testPacsBtn.addClickListener(e -> testPacsConnection());
        
        pacsSection.add(pacsTitle, pacsForm, testPacsBtn);

        // Section 4 - Tests
        VerticalLayout testsSection = new VerticalLayout();
        testsSection.addClassNames("border-radius-m", "p-m", "mb-m", "bg-contrast-5pct");
        
        H3 testsTitle = new H3("Tests");
        testsTitle.addClassNames("mb-s", "text-primary");
        
        Button testAllBtn = new Button("Tester toutes les connexions", VaadinIcon.PLUG.create());
        testAllBtn.addClassNames("primary");
        testAllBtn.addClickListener(e -> testAllConnections());
        
        testsSection.add(testsTitle, testAllBtn);

        // Main save button
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        buttonLayout.addClassNames("mt-l");

        Button saveBtn = new Button("Enregistrer configuration", VaadinIcon.CHECK.create());
        saveBtn.addClassNames("primary");
        saveBtn.addClickListener(e -> saveDicomConfiguration());

        buttonLayout.add(saveBtn);

        // Add all sections to main form
        mainForm.add(mwlSection, modalitySection, pacsSection);

        content.add(title, mainForm, testsSection, buttonLayout);
        return content;
    }

    private VerticalLayout createExamTypesTab() {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(false);

        H2 title = new H2("Types d'examens disponibles");
        title.addClassNames("mb-m");

        // Grid with exam types
        loadExamTypes();
        examTypeGrid.setSizeFull();
        examTypeGrid.setHeight("500px");

        content.add(title, examTypeGrid);
        content.setFlexGrow(1, examTypeGrid);

        return content;
    }

    private Grid<User> createUserGrid() {
        Grid<User> grid = new Grid<>();
        grid.addClassNames("border-radius-m", "striped-rows");
        grid.setHeight("600px"); // Increased height for better visibility

        // Columns
        grid.addColumn(User::getUsername)
                .setHeader("Username")
                .setSortable(true);

        grid.addColumn(user -> user.getFirstName() + " " + user.getLastName())
                .setHeader("Nom complet")
                .setSortable(true);

        grid.addColumn(User::getEmail)
                .setHeader("Email")
                .setSortable(true);

        grid.addColumn(User::getRole)
                .setHeader("Rôle")
                .setSortable(true);

        grid.addColumn(user -> user.getActive() ? "Actif" : "Inactif")
                .setHeader("Statut")
                .setSortable(true);

        grid.addColumn(new ComponentRenderer<>(user -> createUserActions(user)))
                .setHeader("Actions")
                .setSortable(false);

        return grid;
    }

    private Grid<ExamType> createExamTypeGrid() {
        Grid<ExamType> grid = new Grid<>();
        grid.addClassNames("border-radius-m", "striped-rows");

        grid.addColumn(examType -> examType.name())
                .setHeader("Type")
                .setSortable(true);

        grid.addColumn(examType -> examType.toString())
                .setHeader("Modalité")
                .setSortable(true);

        grid.addColumn(examType -> {
            switch (examType) {
                case CT: return "Tomodensitométrie";
                case MRI: return "Imagerie par Résonance Magnétique";
                case RX: return "Radiographie";
                case ECHO: return "Échographie";
                case MAMMO: return "Mammographie";
                case FLUORO: return "Fluoroscopie";
                case PET: return "Tomographie par Émission de Positrons";
                default: return examType.name();
            }
        })
                .setHeader("Description")
                .setSortable(true);

        return grid;
    }

    private Component createUserActions(User user) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(false);

        // Edit button
        Button editBtn = new Button(VaadinIcon.EDIT.create());
        editBtn.addClassNames("small", "icon-button", "tertiary");
        editBtn.getElement().setProperty("title", "Modifier");
        editBtn.addClickListener(e -> openUserDialog(user));

        // Toggle active button
        Button toggleBtn = new Button(user.getActive() ? VaadinIcon.EYE_SLASH.create() : VaadinIcon.EYE.create());
        toggleBtn.addClassNames("small", "icon-button", "secondary");
        toggleBtn.getElement().setProperty("title", user.getActive() ? "Désactiver" : "Activer");
        toggleBtn.addClickListener(e -> toggleUserActive(user));

        // Delete button
        Button deleteBtn = new Button(VaadinIcon.TRASH.create());
        deleteBtn.addClassNames("small", "icon-button", "error");
        deleteBtn.getElement().setProperty("title", "Supprimer");
        deleteBtn.addClickListener(e -> confirmDeleteUser(user));

        actions.add(editBtn, toggleBtn, deleteBtn);
        return actions;
    }

    private void openUserDialog(User user) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(user == null ? "Nouvel utilisateur" : "Modifier utilisateur");
        dialog.setWidth("500px");

        FormLayout form = new FormLayout();

        TextField username = new TextField("Username");
        TextField password = new TextField("Password");
        TextField email = new TextField("Email");
        TextField firstName = new TextField("Prénom");
        TextField lastName = new TextField("Nom");
        com.vaadin.flow.component.combobox.ComboBox<UserRole> role = new com.vaadin.flow.component.combobox.ComboBox<>("Rôle");
        role.setItems(UserRole.values());

        // Pre-fill if editing
        if (user != null) {
            username.setValue(user.getUsername());
            email.setValue(user.getEmail());
            firstName.setValue(user.getFirstName());
            lastName.setValue(user.getLastName());
            role.setValue(user.getRole());
            password.setPlaceholder("Laisser vide pour ne pas changer");
        }

        form.add(username, password, email, firstName, lastName, role);

        Button saveBtn = new Button("Enregistrer", VaadinIcon.CHECK.create());
        saveBtn.addClassNames("primary");
        saveBtn.addClickListener(e -> {
            try {
                if (user == null) {
                    // Create new user
                    User newUser = new User();
                    newUser.setUsername(username.getValue());
                    newUser.setPassword(password.getValue());
                    newUser.setEmail(email.getValue());
                    newUser.setFirstName(firstName.getValue());
                    newUser.setLastName(lastName.getValue());
                    newUser.setRole(role.getValue());
                    newUser.setActive(true);
                    userService.createUser(newUser, password.getValue());
                    Notification.show("Utilisateur créé avec succès");
                } else {
                    // Update existing user
                    user.setUsername(username.getValue());
                    user.setEmail(email.getValue());
                    user.setFirstName(firstName.getValue());
                    user.setLastName(lastName.getValue());
                    user.setRole(role.getValue());
                    if (password.getValue() != null && !password.getValue().isEmpty()) {
                        user.setPassword(password.getValue());
                    }
                    userService.updateUser(user.getId(), user);
                    Notification.show("Utilisateur modifié avec succès");
                }
                loadUsers();
                dialog.close();
            } catch (Exception ex) {
                Notification.show("Erreur: " + ex.getMessage());
            }
        });

        Button cancelBtn = new Button("Annuler", e -> dialog.close());

        HorizontalLayout buttonLayout = new HorizontalLayout(saveBtn, cancelBtn);
        dialog.add(form, buttonLayout);
        dialog.open();
    }

    private void toggleUserActive(User user) {
        user.setActive(!user.getActive());
        userService.updateUser(user.getId(), user);
        loadUsers();
        Notification.show(user.getActive() ? "Utilisateur activé" : "Utilisateur désactivé");
    }

    private void confirmDeleteUser(User user) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Confirmation de suppression");
        
        VerticalLayout content = new VerticalLayout();
        content.add("Êtes-vous sûr de vouloir supprimer l'utilisateur " + user.getUsername() + " ?");
        
        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setSpacing(true);
        
        Button confirmBtn = new Button("Supprimer", e -> {
            userService.deleteUser(user.getId());
            loadUsers();
            Notification.show("Utilisateur supprimé avec succès");
            dialog.close();
        });
        confirmBtn.addClassNames("error");
        
        Button cancelBtn = new Button("Annuler", e -> dialog.close());
        
        buttons.add(confirmBtn, cancelBtn);
        content.add(buttons);
        
        dialog.add(content);
        dialog.open();
    }

    private void loadUsers() {
        try {
            List<User> users = userService.findAll();
            userGrid.setItems(users);
        } catch (Exception e) {
            Notification.show("Erreur lors du chargement des utilisateurs: " + e.getMessage());
        }
    }

    private void loadExamTypes() {
        List<ExamType> examTypes = Arrays.asList(ExamType.values());
        examTypeGrid.setItems(examTypes);
    }

    private void setDefaultValues() {
        risAeTitle.setValue("RIS_MWL");
        mwlPort.setValue(104);
        pacsAeTitle.setValue("ORTH_PACS");
        pacsIp.setValue("localhost");
        pacsPort.setValue(4242);
    }

    private void saveDicomConfiguration() {
        try {
            // TODO: Implement saving to database or application.properties
            Notification.show("Configuration DICOM enregistrée (à implémenter)");
        } catch (Exception e) {
            Notification.show("Erreur lors de l'enregistrement: " + e.getMessage());
        }
    }

    private void updateMWLStatus() {
        try {
            boolean isRunning = mwlService.isRunning();
            mwlStatusBadge.setText(isRunning ? "Actif" : "Arrêté");
            mwlStatusBadge.getStyle().set("background-color", isRunning ? "#f5f5f5" : "#7f1d1d");
            mwlStatusBadge.getStyle().set("color", isRunning ? "#10b981" : "white");
        } catch (Exception e) {
            mwlStatusBadge.setText("Erreur");
            mwlStatusBadge.getStyle().set("background-color", "#6b7280");
            mwlStatusBadge.getStyle().set("color", "white");
        }
    }

    private void restartMWLServer() {
        try {
            mwlService.stopServer();
            Thread.sleep(1000); // Wait for graceful shutdown
            mwlService.startServer();
            updateMWLStatus();
            Notification.show("Serveur MWL redémarré avec succès");
        } catch (Exception e) {
            Notification.show("Erreur lors du redémarrage du serveur MWL: " + e.getMessage());
        }
    }

    private void testModalityConnection() {
        try {
            boolean success = modalityService.testConnection();
            Notification.show(success ? 
                "Connexion Modality établie avec succès" : 
                "Échec de connexion Modality");
        } catch (Exception e) {
            Notification.show("Erreur lors du test de connexion Modality: " + e.getMessage());
        }
    }

    private void testPacsConnection() {
        try {
            boolean success = pacsService.testConnection();
            Notification.show(success ? 
                "Connexion PACS établie avec succès" : 
                "Échec de connexion PACS");
        } catch (Exception e) {
            Notification.show("Erreur lors du test de connexion PACS: " + e.getMessage());
        }
    }

    private void testAllConnections() {
        Dialog resultsDialog = new Dialog();
        resultsDialog.setHeaderTitle("Résultats des tests de connexion");
        resultsDialog.setWidth("600px");
        
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);
        
        // Test MWL
        boolean mwlRunning = false;
        try {
            mwlRunning = mwlService.isRunning();
        } catch (Exception e) {
            // Ignore for now
        }
        
        // Test Modality
        boolean modalityOk = false;
        try {
            modalityOk = modalityService.testConnection();
        } catch (Exception e) {
            // Ignore for now
        }
        
        // Test PACS
        boolean pacsOk = false;
        try {
            pacsOk = pacsService.testConnection();
        } catch (Exception e) {
            // Ignore for now
        }
        
        // Results
        content.add(new H3("Résultats des tests:"));
        content.add(new Span("• Serveur MWL: " + (mwlRunning ? " Actif" : " Arrêté")));
        content.add(new Span("• Connexion Modality: " + (modalityOk ? " Succès" : " Échec")));
        content.add(new Span("• Connexion PACS: " + (pacsOk ? " Succès" : " Échec")));
        
        Button closeBtn = new Button("Fermer", e -> resultsDialog.close());
        content.add(closeBtn);
        
        resultsDialog.add(content);
        resultsDialog.open();
    }
}
