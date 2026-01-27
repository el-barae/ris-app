package com.application.views;

import com.application.entity.*;
import com.application.repository.ExamRepository;
import com.application.repository.ModalityTypeRepository;
import com.application.repository.PatientRepository;
import com.application.repository.ProcedureCatalogRepository;
import com.application.repository.UserRepository;
import com.application.views.calendar.ExamCalendarView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import jakarta.annotation.security.RolesAllowed;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Route(value = "secretaire", layout = MainLayout.class)
@RouteAlias(value = "exams", layout = MainLayout.class)
@PageTitle("Gestion des examens")
@RolesAllowed({"ADMIN", "MEDECIN", "SECRETAIRE", "RADIOLOGUE"})
public class ExamView extends VerticalLayout {

    private final PatientRepository patientRepo;
    private final ExamRepository examRepo;
    private final UserRepository userRepo;
    private final ProcedureCatalogRepository procedureRepo;
    private final ModalityTypeRepository modalityRepo;

    // Composants UI - Recherche et filtres
    private TextField examSearchField = new TextField();
    private ComboBox<User> medecinFilter = new ComboBox<>();
    private final Span examCountBadge = new Span();
    private ComboBox<String> modalityFilter = new ComboBox<>();
    private ComboBox<ExamStatus> statusFilter = new ComboBox<>();
    private DatePicker dateFilter = new DatePicker();

    // Composants UI - Grille des examens
    private Grid<Exam> examGrid = new Grid<>(Exam.class, false);

    // Composants UI - Formulaire création/modification
    private ComboBox<Patient> patientSelector = new ComboBox<>("Patient");
    private ComboBox<User> medecinSelector = new ComboBox<>("Médecin prescripteur");
    private ComboBox<String> protocolTemplate = new ComboBox<>("Protocole rapide");
    private ComboBox<ProcedureCatalog> procedureSelector = new ComboBox<>("Procédure");
    private TextField procedureName = new TextField("Nom de la procédure");
    private ComboBox<String> modality = new ComboBox<>("Modalité");
    private ComboBox<String> region = new ComboBox<>("Région anatomique");
    private ComboBox<String> lateralite = new ComboBox<>("Latéralité");
    private ComboBox<Priority> priority = new ComboBox<>("Priorité");
    private TextArea instructions = new TextArea("Instructions complémentaires");

    // Section contraste
    private Checkbox withContrast = new Checkbox("Injection de contraste");
    private TextField contrastType = new TextField("Type de produit");
    private TextField injectionRate = new TextField("Débit (ml/s)");
    private TextField contrastVolume = new TextField("Volume (ml)");

    private Button saveButton = new Button("Enregistrer l'examen");
    private Button resetButton = new Button("Réinitialiser");

    private Exam selectedExam;
    private ExamCalendarView examCalendar;

    public ExamView(PatientRepository patientRepo, ExamRepository examRepo, UserRepository userRepo, ProcedureCatalogRepository procedureRepo, ModalityTypeRepository modalityRepo) {
        this.patientRepo = patientRepo;
        this.examRepo = examRepo;
        this.userRepo = userRepo;
        this.procedureRepo = procedureRepo;
        this.modalityRepo = modalityRepo;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // Initialiser le calendrier
        examCalendar = new ExamCalendarView(examRepo);

        // Construction de l'interface
        add(
                createHeader(),
                createMainContent()
        );

        // Chargement initial des données
        updateExamList();
    }

    // ==================== CONSTRUCTION DE L'INTERFACE ====================

    private Component createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidth("97%");
        header.setPadding(true);
        header.setSpacing(true);
        header.setAlignItems(Alignment.CENTER);
        header.getStyle()
                .set("background", "linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%)")
                .set("color", "white")
                .set("box-shadow", "0 4px 6px rgba(0,0,0,0.1)")
                .set("border-radius", "0 0 16px 16px")
                .set("padding-top", "1rem")
                .set("padding-left", "1rem")
                .set("padding-right", "1rem")
                .set("margin-top", "1rem")
                .set("margin-left", "1rem");

        // Icône et titre
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(Alignment.CENTER);
        titleLayout.setSpacing(true);

        Icon examIcon = VaadinIcon.CLIPBOARD.create();
        examIcon.setSize("32px");
        examIcon.getStyle().set("color", "white");

        H2 title = new H2("Gestion des Examens");
        title.getStyle()
                .set("margin", "0")
                .set("color", "white")
                .set("font-weight", "600");

        titleLayout.add(examIcon, title);

        // Badge compteur
        examCountBadge.getStyle()
                .set("background-color", "rgba(255,255,255,0.2)")
                .set("color", "white")
                .set("padding", "0.5rem 1rem")
                .set("border-radius", "2rem")
                .set("font-weight", "600")
                .set("font-size", "14px");

        // Boutons
        Button calendarBtn = new Button("Calendrier", VaadinIcon.CALENDAR.create());
        calendarBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        calendarBtn.getStyle()
                .set("background", "rgba(255,255,255,0.2)")
                .set("color", "white");
        calendarBtn.addClickListener(e -> examCalendar.show());

        Button newExamBtn = new Button("Nouvel Examen", VaadinIcon.PLUS.create());
        newExamBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        newExamBtn.addClickListener(e -> openExamForm(null));

        header.add(titleLayout, examCountBadge);
        header.setFlexGrow(1, titleLayout);
        header.add(calendarBtn, newExamBtn);

        return header;
    }

    private Component createMainContent() {
        VerticalLayout mainContent = new VerticalLayout();
        mainContent.setSizeFull();
        mainContent.setPadding(true);
        mainContent.setSpacing(true);

        mainContent.add(
                createFiltersSection(),
                createExamGrid()
        );

        mainContent.setFlexGrow(1, createExamGrid());

        return mainContent;
    }

    private Component createFiltersSection() {
        HorizontalLayout filtersLayout = new HorizontalLayout();
        filtersLayout.setWidthFull();
        filtersLayout.setSpacing(true);
        filtersLayout.setPadding(true);
        filtersLayout.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "8px")
                .set("margin-bottom", "16px");

        // Champ de recherche
        examSearchField.setPlaceholder("Rechercher (patient, ID, modalité...)");
        examSearchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        examSearchField.setClearButtonVisible(true);
        examSearchField.setValueChangeMode(ValueChangeMode.LAZY);
        examSearchField.addValueChangeListener(e -> updateExamList());
        examSearchField.setWidth("300px");

        // Filtre médecin
        medecinFilter.setPlaceholder("Tous les médecins");
        medecinFilter.setItems(userRepo.findAll().stream()
                .filter(user -> user.getRole() == UserRole.MEDECIN)
                .toList());
        medecinFilter.setItemLabelGenerator(user -> user.getFirstName() + " " + user.getLastName());
        medecinFilter.setClearButtonVisible(true);
        medecinFilter.addValueChangeListener(e -> updateExamList());
        medecinFilter.setWidth("180px");

        // Filtre modalité
        modalityFilter.setPlaceholder("Toutes modalités");
        modalityFilter.setItems("CT", "MR", "US", "CR", "DX");
        modalityFilter.setClearButtonVisible(true);
        modalityFilter.addValueChangeListener(e -> updateExamList());
        modalityFilter.setWidth("150px");

        // Filtre statut
        statusFilter.setPlaceholder("Tous statuts");
        statusFilter.setItems(ExamStatus.values());
        statusFilter.setClearButtonVisible(true);
        statusFilter.addValueChangeListener(e -> updateExamList());
        statusFilter.setWidth("150px");

        // Filtre date
        dateFilter.setPlaceholder("Filtrer par date");
        dateFilter.setClearButtonVisible(true);
        dateFilter.addValueChangeListener(e -> updateExamList());
        dateFilter.setWidth("180px");

        Button resetFilters = new Button("Réinitialiser", VaadinIcon.REFRESH.create());
        resetFilters.addClickListener(e -> {
            examSearchField.clear();
            medecinFilter.clear();
            modalityFilter.clear();
            statusFilter.clear();
            dateFilter.clear();
            updateExamList();
        });

        filtersLayout.add(
                examSearchField,
                medecinFilter,
                modalityFilter,
                statusFilter,
                dateFilter,
                resetFilters
        );
        filtersLayout.setFlexGrow(1, examSearchField);
        filtersLayout.setAlignItems(Alignment.CENTER);

        return filtersLayout;
    }

    private Component createExamGrid() {
        examGrid.removeAllColumns();
        examGrid.setSizeFull();
        examGrid.setSelectionMode(Grid.SelectionMode.SINGLE);

        // Configuration des colonnes
        examGrid.addColumn(exam -> exam.getAccessionNumber())
                .setHeader("N° Accession")
                .setWidth("140px")
                .setFlexGrow(0)
                .setSortable(true);

        examGrid.addColumn(exam -> {
                    Patient p = exam.getPatient();
                    return p != null ? p.getLastName() + " " + p.getFirstName() : "N/A";
                })
                .setHeader("Patient")
                .setFlexGrow(1)
                .setSortable(true);

        examGrid.addColumn(exam -> {
                    Patient p = exam.getPatient();
                    return p != null ? p.getPatientId() : "N/A";
                })
                .setHeader("IPP")
                .setWidth("120px")
                .setFlexGrow(0);

        examGrid.addColumn(Exam::getModality)
                .setHeader("Modalité")
                .setWidth("100px")
                .setFlexGrow(0);

        examGrid.addColumn(exam -> {
                    User m = exam.getMedecin();
                    return m != null ? m.getFirstName() + " " + m.getLastName() : "N/A";
                })
                .setHeader("Médecin")
                .setWidth("150px")
                .setFlexGrow(0);

        examGrid.addComponentColumn(exam -> {
                    Span statusBadge = new Span(exam.getStatus() != null ? exam.getStatus().toString() : "N/A");
                    statusBadge.getElement().getThemeList().add("badge");

                    if (exam.getStatus() != null) {
                        switch (exam.getStatus()) {
                            case CREATED:
                                statusBadge.getElement().getThemeList().add("secondary");
                                break;
                            case PLANNED:
                                statusBadge.getElement().getThemeList().add("primary");
                                break;
                            case IN_PROGRESS:
                                statusBadge.getElement().getThemeList().add("contrast");
                                break;
                            case COMPLETED:
                                statusBadge.getElement().getThemeList().add("success");
                                break;
                            case CANCELLED:
                                statusBadge.getElement().getThemeList().add("error");
                                break;
                        }
                    }

                    return statusBadge;
                })
                .setHeader("Statut")
                .setWidth("120px")
                .setFlexGrow(0);

        examGrid.addComponentColumn(exam -> {
                    Span priorityBadge = new Span(exam.getPriority() != null ? exam.getPriority().toString() : "NORMAL");
                    priorityBadge.getElement().getThemeList().add("badge");

                    if (exam.getPriority() == Priority.URGENT) {
                        priorityBadge.getElement().getThemeList().add("error");
                    } else if (exam.getPriority() == Priority.NORMAL) {
                        priorityBadge.getElement().getThemeList().add("primary");
                    }

                    return priorityBadge;
                })
                .setHeader("Priorité")
                .setWidth("100px")
                .setFlexGrow(0);

        examGrid.addColumn(exam -> {
                    if (exam.getScheduledDateTime() != null) {
                        return exam.getScheduledDateTime().format(
                                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                        );
                    }
                    return "N/A";
                })
                .setHeader("Date programmée")
                .setWidth("160px")
                .setFlexGrow(0)
                .setSortable(true);

        examGrid.addComponentColumn(this::createActionButtons)
                .setHeader("Actions")
                .setWidth("150px")
                .setFlexGrow(0);

        examGrid.getStyle()
                .set("border-radius", "8px")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)");

        return examGrid;
    }

    private HorizontalLayout createActionButtons(Exam exam) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);
        actions.setPadding(false);

        Button viewBtn = new Button(VaadinIcon.EYE.create());
        viewBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        viewBtn.getElement().setProperty("title", "Voir les détails");
        viewBtn.addClickListener(e -> openExamDetails(exam));

        Button editBtn = new Button(VaadinIcon.EDIT.create());
        editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        editBtn.getElement().setProperty("title", "Modifier");
        editBtn.addClickListener(e -> openExamForm(exam));

        Button deleteBtn = new Button(VaadinIcon.TRASH.create());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        deleteBtn.getElement().setProperty("title", "Supprimer");
        deleteBtn.addClickListener(e -> confirmDeleteExam(exam));

        actions.add(viewBtn, editBtn, deleteBtn);
        return actions;
    }

    // ==================== FORMULAIRE EXAMEN ====================

    private void openExamForm(Exam exam) {
        Dialog dialog = new Dialog();
        dialog.setWidth("800px");
        dialog.setHeight("90vh");

        boolean isEdit = exam != null;
        selectedExam = exam;

        dialog.setHeaderTitle(isEdit ? "Modifier l'examen" : "Nouvel examen");

        VerticalLayout formContainer = new VerticalLayout();
        formContainer.setPadding(false);
        formContainer.setSpacing(true);

        // Section 1: Patient et Médecin
        FormLayout section1 = new FormLayout();
        section1.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        configurePatientSelector();
        configureMedecinSelector();

        if (isEdit) {
            patientSelector.setValue(exam.getPatient());
            patientSelector.setEnabled(false);
            medecinSelector.setValue(exam.getMedecin());
        }

        Button newPatientBtn = new Button("Nouveau patient", VaadinIcon.PLUS_CIRCLE.create());
        newPatientBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        newPatientBtn.addClickListener(e -> openPatientDialog(dialog));

        section1.add(patientSelector, medecinSelector);
        section1.setColspan(patientSelector, 2);

        // Section 2: Protocole et Procédure
        FormLayout section2 = new FormLayout();
        section2.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        configureProtocolTemplate();
        configureProcedureFields();

        if (isEdit) {
            if (exam.getProcedure() != null) {
                procedureSelector.setValue(exam.getProcedure());
            } else {
                procedureName.setValue(exam.getAdditionalInstructions() != null ? exam.getAdditionalInstructions() : "");
            }
            modality.setValue(exam.getModality());
            priority.setValue(exam.getPriority() != null ? exam.getPriority() : Priority.NORMAL);
        }

        section2.add(procedureSelector, procedureName, modality, region, lateralite, priority);
        section2.setColspan(procedureSelector, 2);
        section2.setColspan(procedureName, 2);

        // Section 3: Contraste
        FormLayout section3 = new FormLayout();
        section3.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 3)
        );

        configureContrastFields();

        section3.add(withContrast, contrastType, injectionRate, contrastVolume);
        section3.setColspan(withContrast, 3);

        // Section 4: Instructions
        instructions.setWidthFull();
        instructions.setHeight("100px");
        instructions.setPlaceholder("Instructions complémentaires pour le radiologue...");

        // Ajout des sections
        formContainer.add(
                createSectionTitle("Patient et Prescription"),
                section1,
                newPatientBtn,
                new Hr(),
                createSectionTitle("Détails de l'Examen"),
                section2,
                new Hr(),
                createSectionTitle("Injection de Contraste"),
                section3,
                new Hr(),
                createSectionTitle("Instructions"),
                instructions
        );

        // Boutons d'action
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.setIcon(VaadinIcon.CHECK.create());
        saveButton.addClickListener(e -> {
            if (saveExam(isEdit)) {
                dialog.close();
                updateExamList();
            }
        });

        resetButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        resetButton.setIcon(VaadinIcon.REFRESH.create());
        resetButton.addClickListener(e -> resetForm());

        Button cancelButton = new Button("Annuler", VaadinIcon.CLOSE.create());
        cancelButton.addClickListener(e -> dialog.close());

        HorizontalLayout buttonBar = new HorizontalLayout(saveButton, resetButton, cancelButton);
        buttonBar.setSpacing(true);
        buttonBar.setPadding(true);
        buttonBar.setJustifyContentMode(JustifyContentMode.END);
        buttonBar.setWidthFull();

        dialog.add(formContainer);
        dialog.getFooter().add(buttonBar);
        dialog.open();
    }

    private H4 createSectionTitle(String title) {
        H4 sectionTitle = new H4(title);
        sectionTitle.getStyle()
                .set("margin", "8px 0")
                .set("color", "var(--lumo-primary-text-color)");
        return sectionTitle;
    }

    // ==================== CONFIGURATION DES CHAMPS ====================

    private void configurePatientSelector() {
        patientSelector.setItems(patientRepo.findAll());
        patientSelector.setItemLabelGenerator(patient ->
                patient.getLastName() + " " + patient.getFirstName() + " (" + patient.getPatientId() + ")"
        );
        patientSelector.setPlaceholder("Sélectionner un patient");
        patientSelector.setWidthFull();
    }

    private void configureMedecinSelector() {
        medecinSelector.setItems(userRepo.findAll().stream()
                .filter(user -> user.getRole() == UserRole.MEDECIN)
                .toList());
        medecinSelector.setItemLabelGenerator(user ->
                "Dr. " + user.getFirstName() + " " + user.getLastName()
        );
        medecinSelector.setPlaceholder("Sélectionner un médecin");
        medecinSelector.setWidthFull();
    }

    private void configureProtocolTemplate() {
        protocolTemplate.setItems(
                "CT Abdomen Embolie",
                "CT Abdomen Rachis",
                "CT Thorax Standard",
                "IRM Cérébrale",
                "Radio Thorax",
                "Échographie Abdominale"
        );
        protocolTemplate.setPlaceholder("Sélectionner un protocole");
        protocolTemplate.setClearButtonVisible(true);
        protocolTemplate.addValueChangeListener(e -> applyProtocol(e.getValue()));
    }

    private void configureProcedureFields() {
        procedureSelector.setItems(procedureRepo.findAllWithModality());
        procedureSelector.setItemLabelGenerator(procedure -> {
            StringBuilder label = new StringBuilder(procedure.getName());
            if (procedure.getRegion() != null) {
                label.append(" (" + procedure.getRegion() + ")");
            }
            return label.toString();
        });
        procedureSelector.setPlaceholder("Sélectionner une procédure");
        procedureSelector.setClearButtonVisible(true);
        procedureSelector.addValueChangeListener(e -> {
            boolean showCustomFields = e.getValue() == null;
            procedureName.setVisible(showCustomFields);
            region.setVisible(showCustomFields);
            lateralite.setVisible(showCustomFields);
            withContrast.setVisible(showCustomFields);
            if (e.getValue() != null) {
                if (e.getValue().getModalityType() != null) {
                    modality.setValue(e.getValue().getModalityType().getCode());
                }
                region.setValue(e.getValue().getRegion());
                lateralite.setValue(e.getValue().getLaterality() != null ? e.getValue().getLaterality() : "N/A");
                withContrast.setValue(e.getValue().getContrastRequired());
                if (e.getValue().getContrastRequired()) {
                    contrastType.setValue(e.getValue().getContrastType());
                    injectionRate.setValue(e.getValue().getInjectionRate());
                    contrastVolume.setValue(e.getValue().getContrastVolume());
                }
            }
        });

        procedureName.setPlaceholder("Ex: Scanner thoracique avec injection");
        procedureName.setVisible(false);

        // Charger les modalités depuis la base de données
        List<ModalityType> modalities = modalityRepo.findAllActiveOrdered();
        modality.setItems(modalities.stream().map(ModalityType::getCode).toList());
        modality.setPlaceholder("Sélectionner");

        region.setItems("Head", "Neck", "Chest", "Abdomen", "Pelvis", "Spine", "Extremity");
        region.setPlaceholder("Sélectionner");
        region.setVisible(false);

        lateralite.setItems("N/A", "Gauche", "Droite", "Bilatéral");
        lateralite.setValue("N/A");
        lateralite.setVisible(false);

        priority.setItems(Priority.values());
        priority.setValue(Priority.NORMAL);
    }

    private void configureContrastFields() {
        contrastType.setVisible(false);
        injectionRate.setVisible(false);
        contrastVolume.setVisible(false);

        withContrast.addValueChangeListener(e -> {
            boolean visible = e.getValue();
            contrastType.setVisible(visible);
            injectionRate.setVisible(visible);
            contrastVolume.setVisible(visible);
        });

        contrastType.setPlaceholder("Ex: Iohexol 350");
        injectionRate.setPlaceholder("Ex: 4");
        contrastVolume.setPlaceholder("Ex: 100");
        
        withContrast.setVisible(false);
    }

    // ==================== LOGIQUE MÉTIER ====================

    private void applyProtocol(String protocol) {
        if (protocol == null) return;

        procedureName.setValue(protocol);

        if (protocol.contains("CT")) {
            modality.setValue("CT");
        } else if (protocol.contains("IRM")) {
            modality.setValue("MR");
        } else if (protocol.contains("Radio")) {
            modality.setValue("CR");
        } else if (protocol.contains("Échographie")) {
            modality.setValue("US");
        }

        if (protocol.contains("Abdomen")) region.setValue("Abdomen");
        if (protocol.contains("Thorax") || protocol.contains("Chest")) region.setValue("Chest");
        if (protocol.contains("Cérébrale")) region.setValue("Head");

        if (protocol.contains("Embolie")) {
            withContrast.setValue(true);
            contrastType.setValue("Iohexol 350");
            injectionRate.setValue("4");
            contrastVolume.setValue("100");
            priority.setValue(Priority.URGENT);
        }
    }

    private boolean saveExam(boolean isEdit) {
        // Validation
        if (patientSelector.getValue() == null) {
            Notification.show("Veuillez sélectionner un patient", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }

        if (medecinSelector.getValue() == null) {
            Notification.show("Veuillez sélectionner un médecin", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }

        if (modality.getValue() == null) {
            Notification.show("Veuillez sélectionner une modalité", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }

        Exam exam = isEdit ? selectedExam : new Exam();

        exam.setPatient(patientSelector.getValue());
        exam.setMedecin(medecinSelector.getValue());
        exam.setModality(modality.getValue());
        // Temporairement défini à maintenant pour éviter l'erreur de contrainte NOT NULL
        exam.setScheduledDateTime(java.time.LocalDateTime.now());
        exam.setStatus(isEdit ? exam.getStatus() : ExamStatus.CREATED);
        exam.setPriority(priority.getValue());

        if (!isEdit) {
            exam.setAccessionNumber("ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }

        // Créer ou trouver la procédure
        ProcedureCatalog procedure = null;
        if (procedureSelector.getValue() != null) {
            procedure = procedureSelector.getValue();
        } else if (procedureName.getValue() != null && !procedureName.getValue().isEmpty()) {
            // Créer une nouvelle procédure
            procedure = new ProcedureCatalog();
            procedure.setName(procedureName.getValue());
            
            // Trouver la modalité correspondante
            Optional<ModalityType> modalityType = modalityRepo.findByCode(modality.getValue());
            if (modalityType.isPresent()) {
                procedure.setModalityType(modalityType.get());
            } else {
                // Créer une modalité par défaut si elle n'existe pas
                ModalityType newModality = new ModalityType();
                newModality.setCode(modality.getValue());
                newModality.setName(modality.getValue());
                newModality.setIsActive(true);
                newModality = modalityRepo.save(newModality);
                procedure.setModalityType(newModality);
            }
            
            procedure.setRegion(region.getValue());
            procedure.setLaterality(lateralite.getValue() != null && !lateralite.getValue().equals("N/A") ? lateralite.getValue() : null);
            procedure.setContrastRequired(withContrast.getValue());
            if (withContrast.getValue()) {
                procedure.setContrastType(contrastType.getValue());
                procedure.setInjectionRate(injectionRate.getValue());
                procedure.setContrastVolume(contrastVolume.getValue());
            }
            procedure.setDescription("Procédure créée automatiquement depuis la vue examens");
            procedure = procedureRepo.save(procedure);
        }
        
        exam.setProcedure(procedure);
        exam.setAdditionalInstructions(instructions.getValue());

        // Définir le type d'examen
        if (modality.getValue() != null) {
            switch (modality.getValue()) {
                case "CT": exam.setExamType(ExamType.CT); break;
                case "MR": exam.setExamType(ExamType.MRI); break;
                case "CR": case "DX": exam.setExamType(ExamType.RX); break;
                case "US": exam.setExamType(ExamType.ECHO); break;
                default: exam.setExamType(ExamType.CT);
            }
        }

        examRepo.save(exam);

        String message = isEdit ? "Examen modifié avec succès" : "✅ Examen créé et ajouté à la Worklist";
        Notification.show(message, 3000, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        resetForm();
        return true;
    }

    private void resetForm() {
        patientSelector.clear();
        medecinSelector.clear();
        procedureSelector.clear();
        procedureName.clear();
        modality.clear();
        region.clear();
        lateralite.setValue("N/A");
        priority.setValue(Priority.NORMAL);
        withContrast.setValue(false);
        contrastType.clear();
        injectionRate.clear();
        contrastVolume.clear();
        instructions.clear();
        
        // Réinitialiser la visibilité des champs
        procedureName.setVisible(false);
        region.setVisible(false);
        lateralite.setVisible(false);
        withContrast.setVisible(false);
    }

    private void updateExamList() {
        List<Exam> exams = examRepo.findAllWithRelations();

        // Application des filtres
        exams = exams.stream()
                .filter(exam -> {
                    // Filtre recherche texte
                    if (examSearchField.getValue() != null && !examSearchField.getValue().trim().isEmpty()) {
                        String search = examSearchField.getValue().toLowerCase().trim();
                        boolean matches = false;

                        if (exam.getAccessionNumber() != null && exam.getAccessionNumber().toLowerCase().contains(search)) matches = true;
                        if (exam.getPatient() != null) {
                            String patientName = (exam.getPatient().getLastName() + " " + exam.getPatient().getFirstName()).toLowerCase();
                            if (patientName.contains(search)) matches = true;
                            if (exam.getPatient().getPatientId() != null && exam.getPatient().getPatientId().toLowerCase().contains(search)) matches = true;
                        }
                        if (exam.getModality() != null && exam.getModality().toLowerCase().contains(search)) matches = true;

                        if (!matches) return false;
                    }

                    // Filtre médecin
                    if (medecinFilter.getValue() != null && !exam.getMedecin().equals(medecinFilter.getValue())) {
                        return false;
                    }

                    // Filtre modalité
                    if (modalityFilter.getValue() != null && !modalityFilter.getValue().equals(exam.getModality())) {
                        return false;
                    }

                    // Filtre statut
                    if (statusFilter.getValue() != null && !statusFilter.getValue().equals(exam.getStatus())) {
                        return false;
                    }

                    // Filtre date
                    if (dateFilter.getValue() != null && exam.getScheduledDateTime() != null) {
                        LocalDate examDate = exam.getScheduledDateTime().toLocalDate();
                        if (!examDate.equals(dateFilter.getValue())) {
                            return false;
                        }
                    }

                    return true;
                })
                .sorted((e1, e2) -> {
                    if (e1.getScheduledDateTime() == null) return 1;
                    if (e2.getScheduledDateTime() == null) return -1;
                    return e2.getScheduledDateTime().compareTo(e1.getScheduledDateTime());
                })
                .collect(Collectors.toList());

        examGrid.setItems(exams);
        updateExamCount();
    }

    private void updateExamCount() {
        int count = examGrid.getListDataView().getItemCount();
        examCountBadge.setText(count + " examen" + (count > 1 ? "s" : ""));
    }

    // ==================== DIALOGUES ====================

    private void openPatientDialog(Dialog parentDialog) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Nouveau patient");
        dialog.setWidth("500px");

        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        TextField patientId = new TextField("IPP (ID Hôpital)");
        TextField lastName = new TextField("Nom");
        TextField firstName = new TextField("Prénom");
        DatePicker dob = new DatePicker("Date de naissance");
        ComboBox<Gender> gender = new ComboBox<>("Sexe");
        gender.setItems(Gender.values());

        formLayout.add(patientId, lastName, firstName, dob, gender);
        formLayout.setColspan(patientId, 2);

        Button saveBtn = new Button("Enregistrer", e -> {
            if (lastName.getValue() == null || lastName.getValue().trim().isEmpty()) {
                Notification.show("Le nom est obligatoire", 3000, Notification.Position.MIDDLE);
                return;
            }

            Patient p = new Patient();
            p.setPatientId(patientId.getValue());
            p.setLastName(lastName.getValue());
            p.setFirstName(firstName.getValue());
            p.setDateOfBirth(dob.getValue());
            p.setGender(gender.getValue());

            patientRepo.save(p);

            // Rafraîchir le sélecteur de patients
            configurePatientSelector();
            patientSelector.setValue(p);

            dialog.close();
            Notification.show("Patient créé avec succès", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Annuler", e -> dialog.close());

        HorizontalLayout buttons = new HorizontalLayout(saveBtn, cancelBtn);
        buttons.setJustifyContentMode(JustifyContentMode.END);

        VerticalLayout layout = new VerticalLayout(formLayout, buttons);
        dialog.add(layout);
        dialog.open();
    }

    private void openExamDetails(Exam exam) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Détails de l'examen");
        dialog.setWidth("700px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);

        // Section Patient
        VerticalLayout patientSection = new VerticalLayout();
        patientSection.setPadding(false);
        patientSection.setSpacing(false);

        H4 patientTitle = new H4("Informations Patient");
        patientTitle.getStyle().set("color", "var(--lumo-primary-text-color)");

        Patient patient = exam.getPatient();
        if (patient != null) {
            patientSection.add(
                    patientTitle,
                    createDetailRow("IPP", patient.getPatientId()),
                    createDetailRow("Nom", patient.getLastName() + " " + patient.getFirstName()),
                    createDetailRow("Date de naissance", patient.getDateOfBirth() != null ?
                            patient.getDateOfBirth().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A"),
                    createDetailRow("Sexe", patient.getGender() != null ? patient.getGender().toString() : "N/A")
            );
        }

        // Section Examen
        VerticalLayout examSection = new VerticalLayout();
        examSection.setPadding(false);
        examSection.setSpacing(false);

        H4 examTitle = new H4("Informations Examen");
        examTitle.getStyle().set("color", "var(--lumo-primary-text-color)");

        examSection.add(
                examTitle,
                createDetailRow("N° Accession", exam.getAccessionNumber()),
                createDetailRow("Modalité", exam.getModality()),
                createDetailRow("Médecin prescripteur", exam.getMedecin() != null ?
                        "Dr. " + exam.getMedecin().getFirstName() + " " + exam.getMedecin().getLastName() : "N/A"),
                createDetailRow("Statut", exam.getStatus() != null ? exam.getStatus().toString() : "N/A"),
                createDetailRow("Priorité", exam.getPriority() != null ? exam.getPriority().toString() : "NORMAL"),
                createDetailRow("Date programmée", exam.getScheduledDateTime() != null ?
                        exam.getScheduledDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A")
        );

        // Section Instructions
        String instructionsText = "";
        try {
            if (exam.getProcedure() != null) {
                // Accès sécurisé aux propriétés de la procédure
                String procedureName = exam.getProcedure().getName();
                if (procedureName != null) {
                    instructionsText = procedureName;
                }
                
                String description = exam.getProcedure().getDescription();
                if (description != null && !description.isEmpty()) {
                    instructionsText += "\n" + description;
                }
                
                // Afficher le code de la modalité
                if (exam.getProcedure().getModalityType() != null) {
                    instructionsText += "\n[" + exam.getProcedure().getModalityType().getCode() + "]";
                }
                
                Boolean contrastRequired = exam.getProcedure().getContrastRequired();
                if (contrastRequired != null && contrastRequired) {
                    instructionsText += "\n\nInjection de contraste requise";
                    String contrastType = exam.getProcedure().getContrastType();
                    if (contrastType != null) {
                        instructionsText += " - " + contrastType;
                    }
                }
            }
            
            String additionalInstructions = exam.getAdditionalInstructions();
            if (additionalInstructions != null && !additionalInstructions.isEmpty()) {
                if (!instructionsText.isEmpty()) {
                    instructionsText += "\n\n";
                }
                instructionsText += additionalInstructions;
            }
        } catch (Exception e) {
            // En cas d'erreur de chargement lazy, afficher un message par défaut
            instructionsText = exam.getAdditionalInstructions() != null ? 
                exam.getAdditionalInstructions() : "Informations de procédure non disponibles";
        }
        
        if (!instructionsText.isEmpty()) {
            VerticalLayout instructionsSection = new VerticalLayout();
            instructionsSection.setPadding(false);

            H4 instructionsTitle = new H4("Instructions");
            instructionsTitle.getStyle().set("color", "var(--lumo-primary-text-color)");

            Paragraph instructionsParagraph = new Paragraph(instructionsText);
            instructionsParagraph.getStyle()
                    .set("background", "var(--lumo-contrast-5pct)")
                    .set("padding", "12px")
                    .set("border-radius", "4px")
                    .set("white-space", "pre-wrap");

            instructionsSection.add(instructionsTitle, instructionsParagraph);
            content.add(patientSection, new Hr(), examSection, new Hr(), instructionsSection);
        } else {
            content.add(patientSection, new Hr(), examSection);
        }

        Button closeBtn = new Button("Fermer", e -> dialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dialog.add(content);
        dialog.getFooter().add(closeBtn);
        dialog.open();
    }

    private HorizontalLayout createDetailRow(String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setPadding(false);
        row.setSpacing(true);

        Span labelSpan = new Span(label + ":");
        labelSpan.getStyle()
                .set("font-weight", "600")
                .set("min-width", "180px")
                .set("color", "var(--lumo-secondary-text-color)");

        Span valueSpan = new Span(value != null ? value : "N/A");
        valueSpan.getStyle().set("color", "var(--lumo-body-text-color)");

        row.add(labelSpan, valueSpan);
        return row;
    }

    private void confirmDeleteExam(Exam exam) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Confirmation de suppression");
        dialog.setWidth("500px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);

        Div warningIcon = new Div();
        warningIcon.getStyle()
                .set("text-align", "center")
                .set("font-size", "48px")
                .set("color", "var(--lumo-error-color)")
                .set("margin-bottom", "16px");
        warningIcon.add(VaadinIcon.WARNING.create());

        Paragraph message = new Paragraph(
                "Êtes-vous sûr de vouloir supprimer cet examen ?"
        );
        message.getStyle()
                .set("text-align", "center")
                .set("font-size", "16px");

        if (exam.getPatient() != null) {
            Paragraph details = new Paragraph(
                    "Patient: " + exam.getPatient().getLastName() + " " + exam.getPatient().getFirstName() + "\n" +
                            "Modalité: " + exam.getModality() + "\n" +
                            "N° Accession: " + exam.getAccessionNumber()
            );
            details.getStyle()
                    .set("text-align", "center")
                    .set("background", "var(--lumo-contrast-5pct)")
                    .set("padding", "12px")
                    .set("border-radius", "4px")
                    .set("white-space", "pre-wrap");
            content.add(warningIcon, message, details);
        } else {
            content.add(warningIcon, message);
        }

        Button confirmBtn = new Button("Supprimer définitivement", e -> {
            examRepo.delete(exam);
            updateExamList();
            dialog.close();
            Notification.show("Examen supprimé avec succès", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);
        confirmBtn.setIcon(VaadinIcon.TRASH.create());

        Button cancelBtn = new Button("Annuler", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout buttons = new HorizontalLayout(cancelBtn, confirmBtn);
        buttons.setJustifyContentMode(JustifyContentMode.CENTER);
        buttons.setPadding(true);
        buttons.setSpacing(true);

        dialog.add(content);
        dialog.getFooter().add(buttons);
        dialog.open();
    }
}
