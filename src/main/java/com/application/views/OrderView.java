package com.application.views;

import com.application.entity.*;
import com.application.repository.OrderRepository;
import com.application.repository.ExamRepository;
import com.application.repository.PatientRepository;
import com.application.repository.ProcedureCatalogRepository;
import com.application.repository.ProcedureRepository;
import com.application.repository.UserRepository;
import com.application.repository.HospitalRepository;
import com.application.views.dialog.PatientDialog;
import com.application.util.DicomUidGenerator;
import com.application.service.ExamService;
import com.application.security.SecurityUtils;
import com.application.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.stream.Collectors;

@Route(value = "secretaire", layout = MainLayout.class)
@RouteAlias(value = "orders", layout = MainLayout.class)
@RouteAlias(value = "exams", layout = MainLayout.class)
@PageTitle("Gestion des Ordres")
@RolesAllowed({"ADMIN", "MEDECIN", "SECRETAIRE", "RADIOLOGUE"})
public class OrderView extends VerticalLayout {

    private final OrderRepository orderRepository;
    private final ExamRepository examRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final ProcedureCatalogRepository procedureCatalogRepository;
    private final ProcedureRepository procedureRepository;
    private final ExamService examService;

    // Composants UI - Recherche et filtres
    private TextField orderSearchField = new TextField();
    private ComboBox<User> doctorFilter = new ComboBox<>();
    private ComboBox<Hospital> hospitalFilter = new ComboBox<>();
    private ComboBox<Patient> patientFilter = new ComboBox<>();
    private final Span orderCountBadge = new Span();
    private DatePicker dateFilter = new DatePicker();

    // Composants UI - Grille des orders
    private Grid<Order> orderGrid = new Grid<>(Order.class, false);

    // Order sélectionné
    private Order selectedOrder;

    public OrderView(OrderRepository orderRepository, ExamRepository examRepository, 
                     PatientRepository patientRepository, UserRepository userRepository,
                     HospitalRepository hospitalRepository, ProcedureCatalogRepository procedureCatalogRepository,
                     ProcedureRepository procedureRepository, ExamService examService) {
        this.orderRepository = orderRepository;
        this.examRepository = examRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.hospitalRepository = hospitalRepository;
        this.procedureCatalogRepository = procedureCatalogRepository;
        this.procedureRepository = procedureRepository;
        this.examService = examService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // Construction de l'interface
        add(
                createHeader(),
                createMainContent()
        );

        // Chargement initial des données
        updateOrderList();
    }

    // ==================== CONSTRUCTION DE L'INTERFACE ====================

    private Component createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidth("97%");
        header.setPadding(true);
        header.setSpacing(true);
        header.setAlignItems(Alignment.CENTER);
        header.getStyle()
                .set("background", "linear-gradient(135deg, #10b981 0%, #059669 100%)")
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

        Icon orderIcon = VaadinIcon.CLIPBOARD.create();
        orderIcon.setSize("32px");
        orderIcon.getStyle().set("color", "white");

        H2 title = new H2("Gestion des Ordres");
        title.getStyle()
                .set("margin", "0")
                .set("color", "white")
                .set("font-weight", "600");

        titleLayout.add(orderIcon, title);

        // Badge compteur
        orderCountBadge.getStyle()
                .set("background-color", "rgba(255,255,255,0.2)")
                .set("color", "white")
                .set("padding", "0.5rem 1rem")
                .set("border-radius", "2rem")
                .set("font-weight", "600")
                .set("font-size", "14px");

        Button newOrderBtn = new Button("Nouvel Ordre", VaadinIcon.PLUS.create());
        newOrderBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        newOrderBtn.getStyle()
                .set("background", "#7f1d1d !important")
                .set("color", "white !important")
                .set("border", "none !important");
        newOrderBtn.addClickListener(e -> openOrderForm(null));

        header.add(titleLayout, orderCountBadge);
        header.setFlexGrow(1, titleLayout);
        header.add(newOrderBtn);

        return header;
    }

    private Component createMainContent() {
        VerticalLayout mainContent = new VerticalLayout();
        mainContent.setSizeFull();
        mainContent.setPadding(true);
        mainContent.setSpacing(true);

        mainContent.add(
                createFiltersSection(),
                createOrderGrid()
        );

        mainContent.setFlexGrow(1, createOrderGrid());

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
        orderSearchField.setPlaceholder("Rechercher (ordre, patient, UID...)");
        orderSearchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        orderSearchField.setClearButtonVisible(true);
        orderSearchField.setValueChangeMode(ValueChangeMode.LAZY);
        orderSearchField.addValueChangeListener(e -> updateOrderList());
        orderSearchField.setWidth("300px");

        // Filtre hôpital
        hospitalFilter.setPlaceholder("Tous les hôpitaux");
        hospitalFilter.setItems(hospitalRepository.findAll());
        hospitalFilter.setItemLabelGenerator(Hospital::getName);
        hospitalFilter.setClearButtonVisible(true);
        hospitalFilter.addValueChangeListener(e -> updateOrderList());
        hospitalFilter.setWidth("180px");

        // Filtre médecin
        doctorFilter.setPlaceholder("Tous les médecins");
        doctorFilter.setItems(userRepository.findAll().stream()
                .filter(user -> user.getRole() == UserRole.MEDECIN)
                .toList());
        doctorFilter.setItemLabelGenerator(user -> user.getFirstName() + " " + user.getLastName());
        doctorFilter.setClearButtonVisible(true);
        doctorFilter.addValueChangeListener(e -> updateOrderList());
        doctorFilter.setWidth("180px");

        // Filtre patient
        patientFilter.setPlaceholder("Tous les patients");
        patientFilter.setItems(patientRepository.findAll());
        patientFilter.setItemLabelGenerator(patient -> 
                patient.getLastName() + " " + patient.getFirstName() + " (" + patient.getPatientId() + ")");
        patientFilter.setClearButtonVisible(true);
        patientFilter.addValueChangeListener(e -> updateOrderList());
        patientFilter.setWidth("200px");

        // Filtre date
        dateFilter.setPlaceholder("Filtrer par date");
        dateFilter.setClearButtonVisible(true);
        dateFilter.addValueChangeListener(e -> updateOrderList());
        dateFilter.setWidth("180px");

        Button resetFilters = new Button("Réinitialiser", VaadinIcon.REFRESH.create());
        resetFilters.addClickListener(e -> {
            orderSearchField.clear();
            hospitalFilter.clear();
            doctorFilter.clear();
            patientFilter.clear();
            dateFilter.clear();
            updateOrderList();
        });

        filtersLayout.add(
                orderSearchField,
                hospitalFilter,
                doctorFilter,
                patientFilter,
                dateFilter,
                resetFilters
        );
        filtersLayout.setFlexGrow(1, orderSearchField);
        filtersLayout.setAlignItems(Alignment.CENTER);

        return filtersLayout;
    }

    private Component createOrderGrid() {
        orderGrid.removeAllColumns();
        orderGrid.setSizeFull();
        orderGrid.setSelectionMode(Grid.SelectionMode.SINGLE);

        // Configuration des colonnes
        orderGrid.addColumn(Order::getAccessionNumber)
                .setHeader("N° Accession")
                .setWidth("140px")
                .setFlexGrow(0)
                .setSortable(true);

        orderGrid.addColumn(Order::getStudyInstanceUID)
                .setHeader("Study Instance UID")
                .setWidth("200px")
                .setFlexGrow(0);

        orderGrid.addColumn(order -> {
                    try {
                        Hospital h = order.getHospital();
                        return h != null ? h.getName() : "N/A";
                    } catch (Exception e) {
                        return "N/A";
                    }
                })
                .setHeader("Hôpital")
                .setFlexGrow(1)
                .setSortable(true);

        orderGrid.addColumn(order -> {
                    try {
                        User d = order.getDoctor();
                        return d != null ? "Dr. " + d.getFirstName() + " " + d.getLastName() : "N/A";
                    } catch (Exception e) {
                        return "N/A";
                    }
                })
                .setHeader("Médecin")
                .setWidth("180px")
                .setFlexGrow(0);

        orderGrid.addColumn(order -> {
                    try {
                        Patient p = order.getPatient();
                        return p != null ? p.getLastName() + " " + p.getFirstName() : "N/A";
                    } catch (Exception e) {
                        return "N/A";
                    }
                })
                .setHeader("Patient")
                .setFlexGrow(1)
                .setSortable(true);

        orderGrid.addColumn(order -> {
                    try {
                        return order.getExams() != null ? order.getExams().size() : 0;
                    } catch (Exception e) {
                        return 0; // Fallback si lazy loading échoue
                    }
                })
                .setHeader("Nb. Examens")
                .setWidth("120px")
                .setFlexGrow(0);

        orderGrid.addColumn(order -> {
                    if (order.getCreatedAt() != null) {
                        return order.getCreatedAt().format(
                                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                        );
                    }
                    return "N/A";
                })
                .setHeader("Date création")
                .setWidth("160px")
                .setFlexGrow(0)
                .setSortable(true);

        orderGrid.addComponentColumn(this::createActionButtons)
                .setHeader("Actions")
                .setWidth("200px")
                .setFlexGrow(0);

        orderGrid.getStyle()
                .set("border-radius", "8px")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)");

        return orderGrid;
    }

    private HorizontalLayout createActionButtons(Order order) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);
        actions.setPadding(false);

        Button viewBtn = new Button(VaadinIcon.EYE.create());
        viewBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        viewBtn.getElement().setProperty("title", "Voir les détails");
        viewBtn.addClickListener(e -> openOrderDetails(order));

        Button addExamBtn = new Button(VaadinIcon.PLUS.create());
        addExamBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        addExamBtn.getElement().setProperty("title", "Ajouter un examen");
        addExamBtn.addClickListener(e -> openExamForm(order, null));

        Button editBtn = new Button(VaadinIcon.EDIT.create());
        editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        editBtn.getElement().setProperty("title", "Modifier l'ordre");
        editBtn.addClickListener(e -> openOrderForm(order));

        Button deleteBtn = new Button(VaadinIcon.TRASH.create());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        deleteBtn.getElement().setProperty("title", "Supprimer");
        deleteBtn.addClickListener(e -> confirmDeleteOrder(order));

        actions.add(viewBtn, addExamBtn, editBtn, deleteBtn);
        return actions;
    }

    // ==================== FORMULAIRE ORDRE COMPLET AVEC EXAMS ====================

    private void openOrderForm(Order order) {
        Dialog dialog = new Dialog();
        dialog.setWidth("1200px");
        dialog.setHeight("90vh");

        boolean isEdit = order != null;
        selectedOrder = order;

        dialog.setHeaderTitle(isEdit ? "Modifier l'ordre" : "📋 Nouvel ordre radiologique");

        VerticalLayout formContainer = new VerticalLayout();
        formContainer.setPadding(false);
        formContainer.setSpacing(true);

        // Liste temporaire pour stocker les examens avant sauvegarde
        List<Exam> tempExams = new ArrayList<>();
        if (isEdit && order != null && order.getExams() != null) {
            tempExams.addAll(order.getExams());
        }

        // Section 1: Informations de l'ordre (Patient et Médecin)
        VerticalLayout orderInfoSection = new VerticalLayout();
        orderInfoSection.setSpacing(false);
        orderInfoSection.setPadding(false);
        
        H4 orderInfoTitle = new H4("📝 Informations de l'ordre");
        orderInfoTitle.getStyle()
                .set("margin", "8px 0")
                .set("color", "var(--lumo-primary-text-color)")
                .set("border-bottom", "2px solid var(--lumo-primary-color)")
                .set("padding-bottom", "8px");

        FormLayout orderForm = new FormLayout();
        orderForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );

        // Sélection patient avec bouton d'ajout
        ComboBox<Patient> patientSelector = new ComboBox<>("Patient *");
        patientSelector.setItems(patientRepository.findAll());
        patientSelector.setItemLabelGenerator(patient -> 
                patient.getLastName() + " " + patient.getFirstName() + " (" + patient.getPatientId() + ")");
        patientSelector.setPlaceholder(" Rechercher un patient...");
        patientSelector.setWidthFull();
        patientSelector.setRequired(true);
        patientSelector.setClearButtonVisible(true);
        if (isEdit && order != null) {
            patientSelector.setValue(order.getPatient());
        }

        // Bouton pour ajouter un nouveau patient
        Button addPatientBtn = new Button("Ajouter un patient", VaadinIcon.PLUS.create());
        addPatientBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        addPatientBtn.getElement().setProperty("title", "Créer un nouveau patient");
        addPatientBtn.addClickListener(e -> openPatientDialog(patientSelector));

        // Layout horizontal pour le patient et le bouton d'ajout
        HorizontalLayout patientLayout = new HorizontalLayout();
        patientLayout.setWidthFull();
        patientLayout.setSpacing(true);
        patientLayout.setAlignItems(Alignment.END);
        patientLayout.setFlexGrow(1, patientSelector);
        patientLayout.add(patientSelector, addPatientBtn);

        // Sélection médecin
        ComboBox<User> doctorSelector = new ComboBox<>("Médecin prescripteur *");
        doctorSelector.setItems(userRepository.findAll().stream()
                .filter(user -> user.getRole() == UserRole.MEDECIN)
                .toList());
        doctorSelector.setItemLabelGenerator(user -> "👨‍⚕️ Dr. " + user.getFirstName() + " " + user.getLastName());
        doctorSelector.setPlaceholder(" Rechercher un médecin...");
        doctorSelector.setWidthFull();
        doctorSelector.setRequired(true);
        doctorSelector.setClearButtonVisible(true);
        if (isEdit && order != null) {
            doctorSelector.setValue(order.getDoctor());
        }

        // Sélection hôpital (caché - sera affecté automatiquement)
        ComboBox<Hospital> hospitalSelector = new ComboBox<>("Hôpital");
        hospitalSelector.setItems(hospitalRepository.findAll());
        hospitalSelector.setItemLabelGenerator(Hospital::getName);
        hospitalSelector.setPlaceholder("Hôpital automatique");
        hospitalSelector.setWidthFull();
        hospitalSelector.setVisible(false);
        
        // Récupérer l'hôpital de l'utilisateur connecté
        Hospital userHospital = getCurrentUserHospital();
        if (userHospital != null) {
            hospitalSelector.setValue(userHospital);
        }
        if (isEdit && order != null && order.getHospital() != null) {
            hospitalSelector.setValue(order.getHospital());
        }

        orderForm.add(patientLayout, doctorSelector);

        orderInfoSection.add(orderInfoTitle, orderForm);

        // Section 2: Tableau des examens
        VerticalLayout examsSection = new VerticalLayout();
        examsSection.setSpacing(false);
        examsSection.setPadding(false);
        
        H4 examsTitle = new H4("🔬 Examens à réaliser");
        examsTitle.getStyle()
                .set("margin", "16px 0 8px 0")
                .set("color", "var(--lumo-primary-text-color)")
                .set("border-bottom", "2px solid var(--lumo-primary-color)")
                .set("padding-bottom", "8px");

        // Grille des examens avec style amélioré
        Grid<Exam> examsGrid = new Grid<>(Exam.class, false);
        examsGrid.setSizeFull();
        examsGrid.setMaxHeight("250px");
        examsGrid.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("border-radius", "8px");

        examsGrid.addColumn(exam -> exam.getAccessionNumber())
                .setHeader("N° Accession")
                .setWidth("140px")
                .setFlexGrow(0);

        examsGrid.addColumn(exam -> exam.getProcedure() != null ? exam.getProcedure().getName() : "N/A")
                .setHeader("Procédure")
                .setFlexGrow(1);

        examsGrid.addColumn(exam -> exam.getModalityCode())
                .setHeader("Modalité")
                .setWidth("100px")
                .setFlexGrow(0);

        examsGrid.addColumn(exam -> exam.getPriority() != null ? exam.getPriority().toString() : "NORMAL")
                .setHeader("Priorité")
                .setWidth("100px")
                .setFlexGrow(0);

        examsGrid.addComponentColumn(exam -> createExamActionButtons(exam, examsGrid, tempExams))
                .setHeader("Actions")
                .setWidth("120px")
                .setFlexGrow(0);

        // Initialiser la grille avec les examens temporaires
        examsGrid.setItems(tempExams);

        // Create a reference holder for the order
        final Order[] orderRef = {order};

        // Bouton pour ajouter un examen avec style amélioré
        HorizontalLayout addExamLayout = new HorizontalLayout();
        addExamLayout.setWidthFull();
        addExamLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        addExamLayout.setAlignItems(Alignment.CENTER);
        addExamLayout.setPadding(true);
        addExamLayout.getStyle()
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "8px")
                .set("margin-top", "8px");

        Span examCount = new Span("📊 " + (tempExams.size() + " examen(s)"));
        examCount.getStyle()
                .set("font-weight", "600")
                .set("color", "var(--lumo-secondary-text-color)");

        Button addExamBtn = new Button("Ajouter un examen", VaadinIcon.PLUS.create());
        addExamBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addExamBtn.addClickListener(e -> {
            // Vérifier que les informations de base sont remplies
            User doctor = doctorSelector.getValue();
            Patient patient = patientSelector.getValue();
            Hospital hospital = getCurrentUserHospital();
            
            if (doctor == null || patient == null) {
                Notification.show("⚠️ Veuillez d'abord remplir les informations de l'ordre (médecin et patient)", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            if (hospital == null) {
                Notification.show("⚠️ Impossible de déterminer l'hôpital de l'utilisateur", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            
            // Créer l'ordre uniquement s'il n'existe pas encore (sans sauvegarder en BDD)
            if (orderRef[0] == null) {
                Order tempOrder = new Order();
                tempOrder.setStudyInstanceUID(DicomUidGenerator.generateStudyInstanceUID());
                tempOrder.setAccessionNumber(DicomUidGenerator.generateOrderAccessionNumber());
                tempOrder.setHospital(hospital);
                tempOrder.setDoctor(doctor);
                tempOrder.setPatient(patient);
                
                // NE PAS SAUVEGARDER en BDD - garder en mémoire seulement
                orderRef[0] = tempOrder;
                selectedOrder = tempOrder;
                
                // Update the dialog title to show it's no longer new
                dialog.setHeaderTitle("✏️ Modifier l'ordre radiologique");
            }
            
            openExamFormForOrder(orderRef[0], examsGrid, tempExams);
        });

        addExamLayout.add(examCount, addExamBtn);

        examsSection.add(examsTitle, examsGrid, addExamLayout);

        // Section 3: Boutons d'action
        HorizontalLayout buttonBar = new HorizontalLayout();
        buttonBar.setSpacing(true);
        buttonBar.setPadding(true);
        buttonBar.setJustifyContentMode(JustifyContentMode.END);
        buttonBar.setWidthFull();
        buttonBar.getStyle()
                .set("border-top", "1px solid var(--lumo-contrast-20pct)")
                .set("margin-top", "16px")
                .set("padding-top", "16px");

        Button saveButton = new Button("Enregistrer l'ordre", VaadinIcon.CHECK.create());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        saveButton.getStyle()
                .set("font-weight", "600")
                .set("font-size", "16px");
        saveButton.addClickListener(e -> {
            if (saveOrderWithExams(isEdit, hospitalSelector.getValue(), doctorSelector.getValue(), 
                                  patientSelector.getValue(), tempExams)) {
                dialog.close();
                updateOrderList();
            }
        });

        Button cancelButton = new Button(" Annuler", VaadinIcon.CLOSE.create());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelButton.addClickListener(e -> dialog.close());

        buttonBar.add(cancelButton, saveButton);

        formContainer.add(orderInfoSection, examsSection, buttonBar);

        dialog.add(formContainer);
        dialog.open();
    }

    
    private HorizontalLayout createExamActionButtons(Exam exam, Grid<Exam> examsGrid, List<Exam> tempExams) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);
        actions.setPadding(false);

        Button editBtn = new Button(VaadinIcon.EDIT.create());
        editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        editBtn.getElement().setProperty("title", "Modifier");
        editBtn.addClickListener(e -> openExamFormForOrder(selectedOrder, examsGrid, tempExams));

        Button deleteBtn = new Button(VaadinIcon.TRASH.create());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
        deleteBtn.getElement().setProperty("title", "Supprimer");
        deleteBtn.addClickListener(e -> {
            // Remove from temporary list instead of order
            tempExams.remove(exam);
            // Refresh the grid with temporary list
            examsGrid.setItems(tempExams);
            
            // Find and update the exam count span in the parent layout
            examsGrid.getParent().ifPresent(parent -> {
                if (parent instanceof VerticalLayout) {
                    VerticalLayout layout = (VerticalLayout) parent;
                    layout.getChildren().forEach(child -> {
                        if (child instanceof HorizontalLayout) {
                            HorizontalLayout hLayout = (HorizontalLayout) child;
                            hLayout.getChildren().forEach(component -> {
                                if (component instanceof Span) {
                                    Span span = (Span) component;
                                    if (span.getText().startsWith("📊")) {
                                        span.setText("📊 " + (tempExams.size() + " examen(s)"));
                                    }
                                }
                            });
                        }
                    });
                }
            });
            
            Notification.show("🗑️ Examen supprimé avec succès", 2000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });

        actions.add(editBtn, deleteBtn);
        return actions;
    }

    private void openExamFormForOrder(Order order, Grid<Exam> examsGrid, List<Exam> tempExams) {
        // Create a simple exam form
        Dialog dialog = new Dialog();
        dialog.setWidth("800px");
        dialog.setHeaderTitle("🔬 Ajouter un examen");

        VerticalLayout formContainer = new VerticalLayout();
        formContainer.setPadding(true);
        formContainer.setSpacing(true);

        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        ComboBox<ProcedureCatalog> procedureSelector = new ComboBox<>("Procédure *");
        procedureSelector.setItems(procedureCatalogRepository.findAllWithModality());
        procedureSelector.setItemLabelGenerator(procedure -> {
            String label = procedure.getName();
            if (procedure.getModalityType() != null) {
                label += " (" + procedure.getModalityType().getCode() + ")";
            }
            return label;
        });
        procedureSelector.setPlaceholder("🔍 Sélectionner une procédure...");
        procedureSelector.setWidthFull();
        procedureSelector.setRequired(true);
        procedureSelector.setClearButtonVisible(true);

        ComboBox<Priority> prioritySelector = new ComboBox<>("Priorité");
        prioritySelector.setItems(Priority.values());
        prioritySelector.setValue(Priority.NORMAL);
        prioritySelector.setWidthFull();

        TextArea instructions = new TextArea("Instructions complémentaires");
        instructions.setPlaceholder("Instructions spécifiques pour cet examen...");
        instructions.setWidthFull();
        instructions.setHeight("100px");

        formLayout.add(procedureSelector, prioritySelector, instructions);

        Button saveButton = new Button("Ajouter l'examen", VaadinIcon.CHECK.create());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> {
            if (procedureSelector.getValue() != null) {
                ProcedureCatalog catalog = procedureSelector.getValue();
                
                // Créer une nouvelle Procedure à partir du catalogue (sans sauvegarder en BDD)
                Procedure procedure = new Procedure();
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
                procedure.setScheduledDurationMinutes(30); // Valeur par défaut
                
                // Créer un nouvel examen (sans sauvegarder en BDD)
                Exam newExam = new Exam();
                newExam.setProcedure(procedure);
                newExam.setModalityType(procedure.getModalityType());
                newExam.setPriority(prioritySelector.getValue());
                newExam.setAdditionalInstructions(instructions.getValue());
                newExam.setAccessionNumber(DicomUidGenerator.generateAccessionNumber());
                newExam.setStudyInstanceUID(DicomUidGenerator.generateSOPInstanceUID());
                newExam.setScheduledDateTime(java.time.LocalDateTime.now().plusHours(1));
                newExam.setStatus(ExamStatus.CREATED);
                
                // Handle case where order is null (new order being created)
                if (order == null) {
                    // Create a temporary order or show error message
                    Notification.show("⚠️ Veuillez d'abord créer l'ordre avant d'ajouter des examens", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }
                
                // Ajouter à la liste temporaire (pas à la BDD)
                tempExams.add(newExam);
                
                // Refresh grid and update count
                examsGrid.setItems(tempExams);
                
                // Mettre à jour le compteur d'examens
                examsGrid.getParent().ifPresent(parent -> {
                    if (parent instanceof VerticalLayout) {
                        VerticalLayout layout = (VerticalLayout) parent;
                        layout.getChildren().forEach(child -> {
                            if (child instanceof HorizontalLayout) {
                                HorizontalLayout hLayout = (HorizontalLayout) child;
                                hLayout.getChildren().forEach(component -> {
                                    if (component instanceof Span) {
                                        Span span = (Span) component;
                                        if (span.getText().startsWith("📊")) {
                                            span.setText("📊 " + (tempExams.size() + " examen(s)"));
                                        }
                                    }
                                });
                            }
                        });
                    }
                });
                
                dialog.close();
                Notification.show("✅ Examen ajouté à la liste", 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } else {
                Notification.show("⚠️ Veuillez sélectionner une procédure", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        Button cancelButton = new Button("❌ Annuler", VaadinIcon.CLOSE.create());
        cancelButton.addClickListener(e -> dialog.close());

        HorizontalLayout buttonBar = new HorizontalLayout(saveButton, cancelButton);
        buttonBar.setSpacing(true);
        buttonBar.setJustifyContentMode(JustifyContentMode.END);
        buttonBar.setWidthFull();

        formContainer.add(formLayout, buttonBar);
        dialog.add(formContainer);
        dialog.open();
    }

    
    private boolean saveOrderWithExams(boolean isEdit, Hospital hospital, User doctor, Patient patient, List<Exam> exams) {
        // Validation
        if (doctor == null) {
            Notification.show("Veuillez sélectionner un médecin", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }

        if (patient == null) {
            Notification.show("Veuillez sélectionner un patient", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }

        // Récupérer l'hôpital de l'utilisateur connecté si non fourni
        if (hospital == null) {
            hospital = getCurrentUserHospital();
            if (hospital == null) {
                Notification.show("Impossible de déterminer l'hôpital de l'utilisateur", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return false;
            }
        }

        Order order = isEdit ? selectedOrder : new Order();
        
        order.setStudyInstanceUID(DicomUidGenerator.generateStudyInstanceUID());
        
        if (!isEdit) {
            order.setAccessionNumber(DicomUidGenerator.generateOrderAccessionNumber());
        }
        
        order.setHospital(hospital);
        order.setDoctor(doctor);
        order.setPatient(patient);

        try {
            // Handle new order (not yet saved in DB)
            if (!isEdit && order.getId() == null) {
                // This is a new order, save it first
                orderRepository.save(order);
            }
            
            // Save exams and associate with order
            if (exams != null && !exams.isEmpty()) {
                for (Exam exam : exams) {
                    // First save the procedure if it's not already saved
                    if (exam.getProcedure() != null && exam.getProcedure().getId() == null) {
                        Procedure savedProcedure = procedureRepository.save(exam.getProcedure());
                        exam.setProcedure(savedProcedure);
                    }
                    
                    // Set order relationship
                    exam.setOrder(order);
                    // Patient and medecin are automatically set via the order
                    exam.setAccessionNumber(DicomUidGenerator.generateAccessionNumber());
                    exam.setStudyInstanceUID(DicomUidGenerator.generateSOPInstanceUID());
                    exam.setScheduledDateTime(java.time.LocalDateTime.now().plusHours(1));
                    exam.setStatus(ExamStatus.CREATED);
                    // Keep the priority that was set in the form
                    if (exam.getPriority() == null) {
                        exam.setPriority(Priority.NORMAL);
                    }
                    
                    examService.createExam(exam);
                    order.addExam(exam);
                }
                // Save order again to update the relationship
                orderRepository.save(order);
            }

            String message = isEdit ? "Ordre modifié avec succès" : "✅ Ordre créé avec " + 
                              (exams != null ? exams.size() : 0) + " examen(s)";
            Notification.show(message, 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            return true;
        } catch (Exception e) {
            Notification.show("Erreur: " + e.getMessage(), 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }
    }

    
    // Méthode pour récupérer l'hôpital de l'utilisateur connecté
    @Transactional(readOnly = true)
    private Hospital getCurrentUserHospital() {
        try {
            // Essayer directement avec le contexte de sécurité
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            System.out.println("DEBUG OrderView: Authentication = " + auth);
            
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
                User currentUser = userDetails.getUser();
                System.out.println("DEBUG OrderView: User from CustomUserDetails - ID: " + currentUser.getId());
                System.out.println("DEBUG OrderView: User hospital: " + currentUser.getHospital());
                return currentUser.getHospital();
            }
            
            // Fallback avec SecurityUtils
            Optional<User> currentUserOpt = SecurityUtils.getCurrentUser();
            if (currentUserOpt.isPresent()) {
                User currentUser = currentUserOpt.get();
                System.out.println("DEBUG: User from SecurityUtils - ID: " + currentUser.getId());
                System.out.println("DEBUG: User hospital: " + currentUser.getHospital());
                return currentUser.getHospital();
            }
            
            // Dernier fallback : session Vaadin
            User sessionUser = (User) VaadinSession.getCurrent().getAttribute("user");
            if (sessionUser != null) {
                System.out.println("DEBUG: User from Vaadin session - ID: " + sessionUser.getId());
                System.out.println("DEBUG: User hospital: " + sessionUser.getHospital());
                return sessionUser.getHospital();
            }
            
            System.out.println("DEBUG: No current user found in any source");
        } catch (Exception e) {
            System.out.println("DEBUG: Exception in getCurrentUserHospital: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // ==================== FORMULAIRE EXAMEN ====================

    private void openExamForm(Order order, Exam exam) {
        Dialog dialog = new Dialog();
        dialog.setWidth("1000px");
        dialog.setHeight("90vh");

        boolean isEdit = exam != null;
        dialog.setHeaderTitle(isEdit ? "Modifier l'examen" : "Nouvel examen pour l'ordre " + order.getAccessionNumber());

        VerticalLayout formContainer = new VerticalLayout();
        formContainer.setPadding(false);
        formContainer.setSpacing(true);

        // Informations de l'ordre (non modifiable)
        HorizontalLayout orderInfo = new HorizontalLayout();
        orderInfo.setWidthFull();
        orderInfo.getStyle()
                .set("background", "#f8f9fa")
                .set("border", "1px solid #dee2e6")
                .set("border-radius", "8px")
                .set("padding", "1rem");

        orderInfo.add(
                new Span("Ordre: " + order.getAccessionNumber()),
                new Span(" | "),
                new Span("Hôpital: " + order.getHospital().getName()),
                new Span(" | "),
                new Span("Médecin: Dr. " + order.getDoctor().getFirstName() + " " + order.getDoctor().getLastName())
        );

        // Formulaire examen simplifié
        FormLayout examForm = new FormLayout();
        examForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        ComboBox<Patient> patientSelector = new ComboBox<>("Patient");
        patientSelector.setItems(patientRepository.findAll());
        patientSelector.setItemLabelGenerator(patient -> 
                patient.getLastName() + " " + patient.getFirstName() + " (" + patient.getPatientId() + ")");
        patientSelector.setPlaceholder("Sélectionner un patient");
        patientSelector.setWidthFull();

        ComboBox<ProcedureCatalog> procedureSelector = new ComboBox<>("Procédure");
        procedureSelector.setItems(procedureCatalogRepository.findAllWithModality());
        procedureSelector.setItemLabelGenerator(procedure -> {
            String label = procedure.getName();
            if (procedure.getModalityType() != null) {
                label += " (" + procedure.getModalityType().getCode() + ")";
            }
            return label;
        });
        procedureSelector.setPlaceholder("Sélectionner une procédure");
        procedureSelector.setWidthFull();

        TextArea instructions = new TextArea("Instructions");
        instructions.setPlaceholder("Instructions complémentaires...");
        instructions.setWidthFull();
        instructions.setHeight("100px");

        examForm.add(patientSelector, procedureSelector, instructions);

        // Boutons d'action
        Button saveExamButton = new Button("Enregistrer l'examen", VaadinIcon.CHECK.create());
        saveExamButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveExamButton.addClickListener(e -> {
            if (saveExam(order, patientSelector.getValue(), procedureSelector.getValue(), instructions.getValue())) {
                dialog.close();
                updateOrderList();
            }
        });

        Button cancelButton = new Button("Annuler", VaadinIcon.CLOSE.create());
        cancelButton.addClickListener(e -> dialog.close());

        HorizontalLayout buttonBar = new HorizontalLayout(saveExamButton, cancelButton);
        buttonBar.setSpacing(true);
        buttonBar.setPadding(true);
        buttonBar.setJustifyContentMode(JustifyContentMode.END);
        buttonBar.setWidthFull();

        formContainer.add(orderInfo, examForm, buttonBar);
        dialog.add(formContainer);
        dialog.open();
    }

    private boolean saveExam(Order order, Patient patient, ProcedureCatalog catalog, String instructions) {
        if (patient == null) {
            Notification.show("Veuillez sélectionner un patient", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }

        if (catalog == null) {
            Notification.show("Veuillez sélectionner une procédure", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }

        // Créer une nouvelle Procedure à partir du catalogue
        Procedure procedure = new Procedure();
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
        procedure.setScheduledDurationMinutes(30); // Valeur par défaut
        
        // Sauvegarder la procédure
        procedure = procedureRepository.save(procedure);

        Exam exam = new Exam();
        exam.setOrder(order);
        exam.setProcedure(procedure);
        exam.setModalityType(procedure.getModalityType());
        exam.setAdditionalInstructions(instructions);
        exam.setAccessionNumber("ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        exam.setStudyInstanceUID(order.getStudyInstanceUID() + "." + UUID.randomUUID().toString().substring(0, 8));
        exam.setScheduledDateTime(LocalDateTime.now().plusHours(1));
        exam.setStatus(ExamStatus.CREATED);
        exam.setPriority(Priority.NORMAL);

        try {
            examService.createExam(exam);
            order.addExam(exam);
            orderRepository.save(order);
            
            Notification.show("✅ Examen ajouté à l'ordre avec succès", 3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            return true;
        } catch (Exception e) {
            Notification.show("Erreur: " + e.getMessage(), 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return false;
        }
    }

    // ==================== DÉTAILS ORDRE ====================

    @Transactional(readOnly = true)
    private void openOrderDetails(Order order) {
        // Recharger l'ordre avec toutes les relations pour éviter LazyInitializationException
        Order fullOrder = orderRepository.findById(order.getId()).orElse(order);
        
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Détails de l'ordre - " + fullOrder.getAccessionNumber());
        dialog.setWidth("1200px");
        dialog.setHeight("90vh");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);

        // Section informations ordre
        VerticalLayout orderSection = new VerticalLayout();
        orderSection.setPadding(false);
        orderSection.setSpacing(false);

        H4 orderTitle = new H4("Informations de l'ordre");
        orderTitle.getStyle().set("color", "var(--lumo-primary-text-color)");

        orderSection.add(
                orderTitle,
                createDetailRow("N° Accession", fullOrder.getAccessionNumber()),
                createDetailRow("Study Instance UID", fullOrder.getStudyInstanceUID()),
                createDetailRow("Hôpital", fullOrder.getHospital() != null ? fullOrder.getHospital().getName() : "N/A"),
                createDetailRow("Médecin", fullOrder.getDoctor() != null ? 
                        "Dr. " + fullOrder.getDoctor().getFirstName() + " " + fullOrder.getDoctor().getLastName() : "N/A"),
                createDetailRow("Date création", fullOrder.getCreatedAt() != null ?
                        fullOrder.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A"),
                createDetailRow("Nombre d'examens", String.valueOf(fullOrder.getExams() != null ? fullOrder.getExams().size() : 0))
        );

        // Section examens
        VerticalLayout examsSection = new VerticalLayout();
        examsSection.setPadding(false);
        examsSection.setSpacing(false);

        H4 examsTitle = new H4("Examens associés");
        examsTitle.getStyle().set("color", "var(--lumo-primary-text-color)");

        Grid<Exam> examsGrid = new Grid<>(Exam.class, false);
        examsGrid.setSizeFull();
        examsGrid.setMaxHeight("400px");

        examsGrid.addColumn(exam -> exam.getAccessionNumber())
                .setHeader("N° Accession")
                .setWidth("140px");

        examsGrid.addColumn(exam -> {
                    Patient p = exam.getPatient();
                    return p != null ? p.getLastName() + " " + p.getFirstName() : "N/A";
                })
                .setHeader("Patient")
                .setFlexGrow(1);

        examsGrid.addColumn(exam -> exam.getModalityCode())
                .setHeader("Modalité")
                .setWidth("100px");

        examsGrid.addComponentColumn(exam -> {
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
                            case SELECTED:
                                statusBadge.getElement().getThemeList().add("primary");
                                break;
                        }
                    }
                    return statusBadge;
                })
                .setHeader("Statut")
                .setWidth("120px");

        if (fullOrder.getExams() != null) {
            examsGrid.setItems(fullOrder.getExams());
        }

        examsSection.add(examsTitle, examsGrid);

        Button closeBtn = new Button("Fermer", e -> dialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        content.add(orderSection, new Hr(), examsSection, closeBtn);
        dialog.add(content);
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

    // ==================== SUPPRESSION ====================

    private void confirmDeleteOrder(Order order) {
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
                "Êtes-vous sûr de vouloir supprimer cet ordre ?\nCela supprimera également tous les examens associés."
        );
        message.getStyle()
                .set("text-align", "center")
                .set("font-size", "16px");

        Paragraph details = new Paragraph(
                "Ordre: " + order.getAccessionNumber() + "\n" +
                "Hôpital: " + (order.getHospital() != null ? order.getHospital().getName() : "N/A") + "\n" +
                "Médecin: " + (order.getDoctor() != null ? 
                        "Dr. " + order.getDoctor().getFirstName() + " " + order.getDoctor().getLastName() : "N/A") + "\n" +
                "Examens: " + (order.getExams() != null ? order.getExams().size() : 0)
        );
        details.getStyle()
                .set("text-align", "center")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("padding", "12px")
                .set("border-radius", "4px")
                .set("white-space", "pre-wrap");

        content.add(warningIcon, message, details);

        Button confirmBtn = new Button("Supprimer définitivement", e -> {
            try {
                orderRepository.deleteById(order.getId());
                updateOrderList();
                dialog.close();
                Notification.show("Ordre supprimé avec succès", 3000, Notification.Position.BOTTOM_START)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            } catch (Exception ex) {
                Notification.show("Erreur: " + ex.getMessage(), 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
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

    // ==================== MISE À JOUR ====================

    private void updateOrderList() {
        List<Order> orders = orderRepository.findAll();

        // Application des filtres
        orders = orders.stream()
                .filter(order -> {
                    // Filtre recherche texte
                    if (orderSearchField.getValue() != null && !orderSearchField.getValue().trim().isEmpty()) {
                        String search = orderSearchField.getValue().toLowerCase().trim();
                        boolean matches = false;

                        if (order.getAccessionNumber() != null && order.getAccessionNumber().toLowerCase().contains(search)) matches = true;
                        if (order.getStudyInstanceUID() != null && order.getStudyInstanceUID().toLowerCase().contains(search)) matches = true;
                        if (order.getHospital() != null && order.getHospital().getName().toLowerCase().contains(search)) matches = true;
                        if (order.getDoctor() != null) {
                            String doctorName = (order.getDoctor().getFirstName() + " " + order.getDoctor().getLastName()).toLowerCase();
                            if (doctorName.contains(search)) matches = true;
                        }

                        if (!matches) return false;
                    }

                    // Filtre hôpital
                    if (hospitalFilter.getValue() != null && !order.getHospital().equals(hospitalFilter.getValue())) {
                        return false;
                    }

                    // Filtre médecin
                    if (doctorFilter.getValue() != null && !order.getDoctor().equals(doctorFilter.getValue())) {
                        return false;
                    }

                    // Filtre patient
                    if (patientFilter.getValue() != null && !order.getPatient().equals(patientFilter.getValue())) {
                        return false;
                    }

                    // Filtre date
                    if (dateFilter.getValue() != null && order.getCreatedAt() != null) {
                        LocalDate orderDate = order.getCreatedAt().toLocalDate();
                        if (!orderDate.equals(dateFilter.getValue())) {
                            return false;
                        }
                    }

                    return true;
                })
                .sorted((o1, o2) -> {
                    if (o1.getCreatedAt() == null) return 1;
                    if (o2.getCreatedAt() == null) return -1;
                    return o2.getCreatedAt().compareTo(o1.getCreatedAt());
                })
                .collect(Collectors.toList());

        orderGrid.setItems(orders);
        updateOrderCount();
    }

    private void updateOrderCount() {
        int count = orderGrid.getListDataView().getItemCount();
        orderCountBadge.setText(count + " ordre" + (count > 1 ? "s" : ""));
    }

    // ==================== GESTION DES PATIENTS ====================

    @Transactional
    private void openPatientDialog(ComboBox<Patient> patientSelector) {
        PatientDialog dialog = new PatientDialog(null, patient -> {
            // Sauvegarder le nouveau patient dans la base de données
            Patient savedPatient = patientRepository.save(patient);
            
            // Rafraîchir la liste des patients dans le ComboBox
            patientSelector.setItems(patientRepository.findAll());
            
            // Sélectionner automatiquement le nouveau patient créé
            patientSelector.setValue(savedPatient);
            
            // Afficher une notification de succès
            Notification.show("✅ Patient créé avec succès: " + 
                    savedPatient.getLastName() + " " + savedPatient.getFirstName(), 
                    3000, Notification.Position.BOTTOM_START)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        
        dialog.open();
    }
}
