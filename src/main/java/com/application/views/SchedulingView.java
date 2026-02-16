package com.application.views;

import com.application.entity.*;
import com.application.repository.*;
import com.application.views.calendar.AdvancedCalendarView;
import com.application.security.SecurityUtils;
import com.application.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import com.vaadin.flow.server.VaadinSession;
import java.util.Optional;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.application.views.MainLayout;
import jakarta.annotation.security.RolesAllowed;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "scheduling", layout = MainLayout.class)
@PageTitle("Planification des examens")
@RolesAllowed({"ADMIN", "MEDECIN", "SECRETAIRE"})
public class SchedulingView extends VerticalLayout {

    private final ExamRepository examRepo;
    private final ScheduleSlotRepository scheduleSlotRepo;
    private final ModalityRepository modalityRepo;
    private final ModalityTypeRepository modalityTypeRepo;
    private final TechnicianRepository technicianRepo;
    private final Grid<Object> schedulingGrid = new Grid<>();
    private AdvancedCalendarView advancedCalendarView;

    // Champs de filtrage
    private TextField patientSearchField = new TextField();
    private ComboBox<ModalityType> modalityFilter = new ComboBox<>();
    private ComboBox<String> statusFilter = new ComboBox<>();
    private ComboBox<Technician> technicianFilter = new ComboBox<>();

    public SchedulingView(ExamRepository examRepo, ScheduleSlotRepository scheduleSlotRepo,
                          ModalityRepository modalityRepo, ModalityTypeRepository modalityTypeRepo, TechnicianRepository technicianRepo) {
        this.examRepo = examRepo;
        this.scheduleSlotRepo = scheduleSlotRepo;
        this.modalityRepo = modalityRepo;
        this.modalityTypeRepo = modalityTypeRepo;
        this.technicianRepo = technicianRepo;

        // Initialize advanced calendar
        advancedCalendarView = new AdvancedCalendarView(examRepo, scheduleSlotRepo, modalityRepo, modalityTypeRepo, technicianRepo);

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("scheduling-view");

        add(createHeader());
        add(createContent());

        refreshScheduleList();
    }

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

        Icon schedulingIcon = VaadinIcon.CALENDAR.create();
        schedulingIcon.setSize("32px");
        schedulingIcon.getStyle().set("color", "white");

        H2 title = new H2("Planification des Examens");
        title.getStyle()
                .set("margin", "0")
                .set("color", "white")
                .set("font-weight", "600");

        titleLayout.add(schedulingIcon, title);

        // Badge compteur pour les examens à planifier
        Span schedulingCountBadge = new Span();
        schedulingCountBadge.getStyle()
                .set("background-color", "rgba(255,255,255,0.2)")
                .set("color", "white")
                .set("padding", "0.5rem 1rem")
                .set("border-radius", "2rem")
                .set("font-weight", "600")
                .set("font-size", "14px");

        Button refreshBtn = new Button("Actualiser", VaadinIcon.REFRESH.create());
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        refreshBtn.getStyle()
                .set("background", "rgba(255,255,255,0.2)")
                .set("color", "white");
        refreshBtn.addClickListener(e -> {
            refreshScheduleList();
            Notification.show("Liste actualisée", 2000, Notification.Position.BOTTOM_END);
        });

        Button calendarBtn = new Button("Calendrier", VaadinIcon.CALENDAR.create());
        calendarBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        calendarBtn.getStyle()
                .set("background", "#7f1d1d !important")
                .set("color", "white !important")
                .set("border", "none !important");
        calendarBtn.addClickListener(e -> advancedCalendarView.open());

        header.add(titleLayout, schedulingCountBadge, refreshBtn, calendarBtn);
        header.setFlexGrow(1, titleLayout);
        header.setFlexGrow(0, schedulingCountBadge);

        // Mettre à jour le badge compteur
        updateSchedulingCountBadge(schedulingCountBadge);

        return header;
    }

    private Component createContent() {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(true);
        content.setSpacing(true);

        configureGrid();

        content.add(createFiltersSection(), schedulingGrid);
        return content;
    }

    private Component createFiltersSection() {
        HorizontalLayout filtersLayout = new HorizontalLayout();
        filtersLayout.setWidthFull();
        filtersLayout.setSpacing(true);
        filtersLayout.setPadding(true);
        filtersLayout.getStyle()
                .set("background-color", "#f8f9fa")
                .set("border-radius", "8px")
                .set("margin-bottom", "1rem");

        // Filtre patient
        patientSearchField.setPlaceholder("Rechercher patient...");
        patientSearchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        patientSearchField.setClearButtonVisible(true);
        patientSearchField.setWidth("200px");
        patientSearchField.setValueChangeMode(ValueChangeMode.LAZY);
        patientSearchField.addValueChangeListener(e -> filterGrid());

        // Filtre modalité
        modalityFilter.setPlaceholder("Toutes les modalités");
        modalityFilter.setItems(modalityTypeRepo.findAll());
        modalityFilter.setItemLabelGenerator(modality -> modality != null ? modality.getCode() : "");
        modalityFilter.setClearButtonVisible(true);
        modalityFilter.setWidth("150px");
        modalityFilter.addValueChangeListener(e -> filterGrid());

        // Filtre statut
        statusFilter.setPlaceholder("Tous les statuts");
        statusFilter.setItems("Tous", "Créé", "Planifié", "En cours", "Terminé", "Annulé");
        statusFilter.setClearButtonVisible(true);
        statusFilter.setWidth("150px");
        statusFilter.setValue("Tous");
        statusFilter.addValueChangeListener(e -> filterGrid());

        // Filtre technicien
        technicianFilter.setPlaceholder("Tous les techniciens");
        technicianFilter.setItems(technicianRepo.findByIsActive(true));
        technicianFilter.setItemLabelGenerator(tech -> tech != null ? tech.getFullName() : "");
        technicianFilter.setClearButtonVisible(true);
        technicianFilter.setWidth("200px");
        technicianFilter.addValueChangeListener(e -> filterGrid());

        // Bouton pour effacer les filtres
        Button clearFiltersBtn = new Button("Effacer", VaadinIcon.CLOSE.create());
        clearFiltersBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        clearFiltersBtn.addClickListener(e -> clearFilters());

        filtersLayout.add(patientSearchField, modalityFilter, statusFilter, technicianFilter, clearFiltersBtn);
        filtersLayout.setFlexGrow(1, patientSearchField);

        return filtersLayout;
    }

    private void configureGrid() {
        schedulingGrid.setSizeFull();
        schedulingGrid.removeAllColumns();
        schedulingGrid.addClassName("scheduling-grid");

        // Colonne Type (Type de modalité)
        schedulingGrid.addColumn(item -> {
                    if (item instanceof Exam) {
                        Exam exam = (Exam) item;
                        return exam.getModalityEntity() != null && exam.getModalityEntity().getModalityType() != null ?
                                exam.getModalityEntity().getModalityType().getCode() : "N/A";
                    } else if (item instanceof ScheduleSlot) {
                        ScheduleSlot slot = (ScheduleSlot) item;
                        return slot.getModalityResource() != null && slot.getModalityResource().getModalityType() != null ?
                                slot.getModalityResource().getModalityType().getCode() : "N/A";
                    }
                    return "N/A";
                })
                .setHeader("Type")
                .setWidth("100px")
                .setFlexGrow(0);

        // Colonne Patient
        schedulingGrid.addColumn(item -> {
                    if (item instanceof Exam) {
                        Exam exam = (Exam) item;
                        return exam.getPatient() != null ?
                                exam.getPatient().getLastName() + " " + exam.getPatient().getFirstName() : "N/A";
                    } else if (item instanceof ScheduleSlot) {
                        ScheduleSlot slot = (ScheduleSlot) item;
                        return slot.getOrderLine() != null && slot.getOrderLine().getPatient() != null ?
                                slot.getOrderLine().getPatient().getLastName() + " " + slot.getOrderLine().getPatient().getFirstName() : "Créneau vide";
                    }
                    return "N/A";
                })
                .setHeader("Patient")
                .setWidth("200px")
                .setFlexGrow(0);

        // Colonne Modalité
        schedulingGrid.addColumn(item -> {
                    if (item instanceof Exam) {
                        Exam exam = (Exam) item;
                        return exam.getModalityEntity() != null ?
                                exam.getModalityEntity().getModalityType().getCode() + " - " + exam.getModalityEntity().getNom() : "Non assigné";
                    } else if (item instanceof ScheduleSlot) {
                        ScheduleSlot slot = (ScheduleSlot) item;
                        return slot.getModalityResource() != null ?
                                slot.getModalityResource().getModalityType().getCode() + " - " + slot.getModalityResource().getNom() : "Non assigné";
                    }
                    return "Non assigné";
                })
                .setHeader("Modalité")
                .setWidth("150px")
                .setFlexGrow(0);

        // Colonne Salle
        schedulingGrid.addColumn(item -> {
                    if (item instanceof Exam) {
                        Exam exam = (Exam) item;
                        return exam.getModalityEntity() != null && exam.getModalityEntity().getRoom() != null ?
                                exam.getModalityEntity().getRoom().getName() : "Non assigné";
                    } else if (item instanceof ScheduleSlot) {
                        ScheduleSlot slot = (ScheduleSlot) item;
                        return slot.getModalityResource() != null && slot.getModalityResource().getRoom() != null ?
                                slot.getModalityResource().getRoom().getName() : "Non assigné";
                    }
                    return "Non assigné";
                })
                .setHeader("Salle")
                .setWidth("100px")
                .setFlexGrow(0);

        // Colonne Statut
        schedulingGrid.addColumn(item -> {
                    if (item instanceof Exam) {
                        Exam exam = (Exam) item;
                        if (exam.getStatus() != null) {
                            switch (exam.getStatus()) {
                                case CREATED:
                                    return "Créé";
                                case PLANNED:
                                    return "Planifié";
                                case SELECTED:
                                    return "Sélectionné";
                                case IN_PROGRESS:
                                    return "En cours";
                                case COMPLETED:
                                    return "Terminé";
                                case CANCELLED:
                                    return "Annulé";
                                default:
                                    return "N/A";
                            }
                        }
                        return "N/A";
                    } else if (item instanceof ScheduleSlot) {
                        ScheduleSlot slot = (ScheduleSlot) item;
                        if (slot.getStatus() != null) {
                            switch (slot.getStatus()) {
                                case SCHEDULED:
                                    return "Planifié";
                                case IN_PROGRESS:
                                    return "En cours";
                                case COMPLETED:
                                    return "Terminé";
                                case CANCELLED:
                                    return "Annulé";
                                case NO_SHOW:
                                    return "Absent";
                                case BLOCKED:
                                    return "Bloqué";
                                default:
                                    return "N/A";
                            }
                        }
                        return "N/A";
                    }
                    return "N/A";
                })
                .setHeader("Statut")
                .setWidth("120px")
                .setFlexGrow(0);

        // Colonne Date/Heure
        schedulingGrid.addColumn(item -> {
                    if (item instanceof Exam) {
                        Exam exam = (Exam) item;
                        return exam.getScheduledDateTime() != null ?
                                exam.getScheduledDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "Non programmé";
                    } else if (item instanceof ScheduleSlot) {
                        ScheduleSlot slot = (ScheduleSlot) item;
                        return slot.getScheduledStartTime() != null ?
                                slot.getScheduledStartTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A";
                    }
                    return "N/A";
                })
                .setHeader("Date/Heure")
                .setWidth("160px")
                .setFlexGrow(0);

        // Colonne Technicien
        schedulingGrid.addColumn(item -> {
                    try {
                        if (item instanceof Exam) {
                            Exam exam = (Exam) item;
                            // Pour les examens, chercher le technicien dans les créneaux associés
                            List<ScheduleSlot> slots = scheduleSlotRepo.findByOrderLine(exam);
                            if (!slots.isEmpty() && slots.get(0).getTechnician() != null) {
                                Technician tech = slots.get(0).getTechnician();
                                // Vérifier si le technicien est initialisé
                                if (tech.getId() != null) {
                                    // Recharger le technicien pour éviter les problèmes de lazy loading
                                    Technician reloadedTech = technicianRepo.findById(tech.getId()).orElse(null);
                                    return reloadedTech != null ? reloadedTech.getFullName() : "Non assigné";
                                }
                            }
                            return "Non assigné";
                        } else if (item instanceof ScheduleSlot) {
                            ScheduleSlot slot = (ScheduleSlot) item;
                            if (slot.getTechnician() != null && slot.getTechnician().getId() != null) {
                                // Recharger le technicien pour éviter les problèmes de lazy loading
                                Technician reloadedTech = technicianRepo.findById(slot.getTechnician().getId()).orElse(null);
                                return reloadedTech != null ? reloadedTech.getFullName() : "Non assigné";
                            }
                            return "Non assigné";
                        }
                    } catch (Exception e) {
                        System.err.println("Erreur lors de l'affichage du technicien: " + e.getMessage());
                        return "Erreur";
                    }
                    return "Non assigné";
                })
                .setHeader("Technicien")
                .setWidth("200px")
                .setFlexGrow(0);

        // Colonne Actions
        schedulingGrid.addComponentColumn(item -> {
                    HorizontalLayout actions = new HorizontalLayout();
                    actions.setSpacing(true);

                    if (item instanceof Exam) {
                        Exam exam = (Exam) item;

                        Button detailsBtn = new Button("", VaadinIcon.INFO.create());
                        detailsBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
                        detailsBtn.addClickListener(e -> showExamDetails(exam));
                        actions.add(detailsBtn);

                        if (exam.getStatus() == ExamStatus.CREATED) {
                            Button scheduleBtn = new Button("Planifier", VaadinIcon.CALENDAR.create());
                            scheduleBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
                            scheduleBtn.getStyle().set("background-color", "darkred").set("color", "white");
                            scheduleBtn.addClickListener(e -> openScheduleDialog(exam));
                            actions.add(scheduleBtn);
                        } else if (exam.getStatus() == ExamStatus.PLANNED) {
                            Button modifyBtn = new Button("Modifier", VaadinIcon.EDIT.create());
                            modifyBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
                            modifyBtn.addClickListener(e -> openScheduleDialog(exam));
                            actions.add(modifyBtn);
                        }
                    } else if (item instanceof ScheduleSlot) {
                        ScheduleSlot slot = (ScheduleSlot) item;

                        Button detailsBtn = new Button("Détails", VaadinIcon.INFO.create());
                        detailsBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
                        detailsBtn.addClickListener(e -> showSlotDetails(slot));
                        actions.add(detailsBtn);

                        Button cancelBtn = new Button("Annuler", VaadinIcon.CLOSE.create());
                        cancelBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
                        cancelBtn.addClickListener(e -> cancelSlot(slot));
                        actions.add(cancelBtn);
                    }

                    return actions;
                })
                .setHeader("Actions")
                .setWidth("200px")
                .setFlexGrow(0);

        // Configuration du data provider
        ListDataProvider<Object> dataProvider = new ListDataProvider<>(List.of());
        schedulingGrid.setDataProvider(dataProvider);
    }

    private void showExamDetails(Exam exam) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Détails de l'examen");

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);

        content.add(new Span("Patient: " + (exam.getPatient() != null ?
                exam.getPatient().getLastName() + " " + exam.getPatient().getFirstName() : "N/A")));
        content.add(new Span("Accession: " + exam.getAccessionNumber()));
        content.add(new Span("Modalité: " + (exam.getModalityEntity() != null ?
                exam.getModalityEntity().getModalityType().getCode() + " - " + exam.getModalityEntity().getNom() : "Non assigné")));
        content.add(new Span("Statut: " + (exam.getStatus() != null ? exam.getStatus().toString() : "N/A")));
        content.add(new Span("Date programmée: " + (exam.getScheduledDateTime() != null ?
                exam.getScheduledDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "Non programmé")));
        content.add(new Span("Priorité: " + (exam.getPriority() != null ? exam.getPriority().toString() : "NORMAL")));
        content.add(new Span("Médecin: " + (exam.getMedecin() != null ?
                exam.getMedecin().getFirstName() + " " + exam.getMedecin().getLastName() : "N/A")));

        dialog.add(content);
        dialog.setConfirmText("Fermer");
        dialog.setCancelButton((com.vaadin.flow.component.Component) null);

        dialog.open();
    }

    private void showSlotDetails(ScheduleSlot slot) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Détails du créneau");

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);

        content.add(new Span("Équipement: " + (slot.getModalityResource() != null ?
                slot.getModalityResource().getNom() + " (" + slot.getModalityResource().getAetitle() + ")" : "N/A")));
        content.add(new Span("Date début: " + (slot.getScheduledStartTime() != null ?
                slot.getScheduledStartTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A")));
        content.add(new Span("Date fin: " + (slot.getScheduledEndTime() != null ?
                slot.getScheduledEndTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A")));
        content.add(new Span("Statut: " + (slot.getStatus() != null ? slot.getStatus().toString() : "N/A")));
        content.add(new Span("Technicien: " + (slot.getTechnician() != null ?
                slot.getTechnician().getFullName() : "Non assigné")));

        dialog.add(content);
        dialog.setConfirmText("Fermer");
        dialog.setCancelButton((com.vaadin.flow.component.Component) null);

        dialog.open();
    }

    private void cancelSlot(ScheduleSlot slot) {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("Annuler le créneau");
        confirmDialog.setText("Êtes-vous sûr de vouloir annuler ce créneau ?");

        confirmDialog.setConfirmText("Oui, annuler");
        confirmDialog.setConfirmButtonTheme("error");
        confirmDialog.setCancelText("Non");

        confirmDialog.addConfirmListener(e -> {
            slot.setStatus(ScheduleSlotStatus.CANCELLED);
            if (slot.getOrderLine() != null) {
                slot.getOrderLine().setStatus(ExamStatus.CREATED);
            }
            scheduleSlotRepo.save(slot);

            Notification.show(
                    "Créneau annulé",
                    3000,
                    Notification.Position.BOTTOM_END
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);

            refreshScheduleList();
        });

        confirmDialog.open();
    }

    private void openScheduleDialog(Exam exam) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Planifier l'examen");

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);

        // Afficher les informations de l'examen
        content.add(new Span("Patient: " + (exam.getPatient() != null ?
                exam.getPatient().getLastName() + " " + exam.getPatient().getFirstName() : "N/A")));
        content.add(new Span("Accession: " + exam.getAccessionNumber()));

        // Sélecteur de modalité (équipement)
        ComboBox<Modality> modalitySelector = new ComboBox<>("Équipement / Modalité");
        modalitySelector.setItems(modalityRepo.findAllActiveOrdered());
        modalitySelector.setItemLabelGenerator(modality -> {
            String code = modality.getModalityType() != null ? modality.getModalityType().getCode() : "N/A";
            String name = modality.getNom() != null ? modality.getNom() : "N/A";
            String aetitle = modality.getAetitle() != null ? modality.getAetitle() : "N/A";
            return code + " - " + name + " (" + aetitle + ")";
        });
        modalitySelector.setPlaceholder("Sélectionner un équipement");
        modalitySelector.setWidthFull();

        // Pré-sélectionner la modalité actuelle si elle existe
        Modality currentModality = exam.getModalityEntity();
        if (currentModality != null) {
            modalitySelector.setValue(currentModality);
        }

        content.add(modalitySelector);

        // DateTimePicker pour la date
        DateTimePicker dateTimePicker = new DateTimePicker("Date et heure de début");
        dateTimePicker.setValue(exam.getScheduledDateTime() != null ?
                exam.getScheduledDateTime() : LocalDateTime.now().plusDays(1));
        dateTimePicker.setWidthFull();
        content.add(dateTimePicker);

        // DateTimePicker pour la date de fin
        DateTimePicker endTimePicker = new DateTimePicker("Date et heure de fin");
        endTimePicker.setValue(dateTimePicker.getValue().plusHours(1));
        endTimePicker.setWidthFull();
        content.add(endTimePicker);

        // Sélecteur de technicien
        ComboBox<Technician> technicianSelector = new ComboBox<>("Technicien");
        technicianSelector.setItems(technicianRepo.findByIsActive(true));
        technicianSelector.setItemLabelGenerator(technician -> {
            if (technician == null) {
                return "";
            }
            String fullName = technician.getFullName() != null ? technician.getFullName() : "";
            String employeeId = technician.getEmployeeId() != null ? " (" + technician.getEmployeeId() + ")" : "";
            return fullName + employeeId;
        });
        technicianSelector.setPlaceholder("Sélectionner un technicien (optionnel)");
        technicianSelector.setWidthFull();
        technicianSelector.setClearButtonVisible(true);

        // Pré-sélectionner le technicien actuel si l'examen est déjà planifié
        if (exam.getStatus() == ExamStatus.PLANNED) {
            List<ScheduleSlot> existingSlots = scheduleSlotRepo.findByOrderLine(exam);
            if (!existingSlots.isEmpty()) {
                ScheduleSlot existingSlot = existingSlots.get(0);
                if (existingSlot.getTechnician() != null) {
                    technicianSelector.setValue(existingSlot.getTechnician());
                }
            }
        }

        content.add(technicianSelector);

        dialog.add(content);
        dialog.setCancelable(true);
        dialog.setConfirmText("Planifier");
        dialog.setConfirmButtonTheme("primary");

        // Déterminer si c'est une création ou une modification
        boolean isModification = exam.getStatus() == ExamStatus.PLANNED;

        dialog.setHeader(isModification ? "Modifier l'examen" : "Planifier l'examen");
        dialog.setConfirmText(isModification ? "Modifier" : "Planifier");
        dialog.setConfirmButtonTheme("primary");

        dialog.addConfirmListener(e -> {
            if (dateTimePicker.getValue() != null && modalitySelector.getValue() != null && endTimePicker.getValue() != null) {
                if (isModification) {
                    // Mode modification : chercher le ScheduleSlot existant et le mettre à jour
                    List<ScheduleSlot> existingSlots = scheduleSlotRepo.findByOrderLine(exam);
                    if (!existingSlots.isEmpty()) {
                        ScheduleSlot slot = existingSlots.get(0);
                        slot.setModalityResource(modalitySelector.getValue());
                        slot.setScheduledStartTime(dateTimePicker.getValue());
                        slot.setScheduledEndTime(endTimePicker.getValue());
                        slot.setTechnician(technicianSelector.getValue());
                        scheduleSlotRepo.save(slot);

                        String technicianName = technicianSelector.getValue() != null ?
                                " avec le technicien " + technicianSelector.getValue().getFullName() : "";

                        Notification.show(
                                "Examen modifié avec succès pour le " +
                                        dateTimePicker.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) +
                                        " avec l'équipement " + modalitySelector.getValue().getNom() +
                                        " (" + modalitySelector.getValue().getModalityType().getCode() + ")" + technicianName,
                                3000,
                                Notification.Position.BOTTOM_END
                        ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    }
                } else {
                    // Mode création : créer un nouveau ScheduleSlot
                    ScheduleSlot slot = new ScheduleSlot();
                    slot.setOrderLine(exam);
                    slot.setModalityResource(modalitySelector.getValue());
                    slot.setScheduledStartTime(dateTimePicker.getValue());
                    slot.setScheduledEndTime(endTimePicker.getValue());
                    slot.setStatus(ScheduleSlotStatus.SCHEDULED);
                    slot.setTechnician(technicianSelector.getValue());

                    scheduleSlotRepo.save(slot);

                    String technicianName = technicianSelector.getValue() != null ?
                            " avec le technicien " + technicianSelector.getValue().getFullName() : "";

                    Notification.show(
                            "Examen planifié avec succès pour le " +
                                    dateTimePicker.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) +
                                    " avec l'équipement " + modalitySelector.getValue().getNom() +
                                    " (" + modalitySelector.getValue().getModalityType().getCode() + ")" + technicianName,
                            3000,
                            Notification.Position.BOTTOM_END
                    ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }

                // Mettre à jour l'examen dans tous les cas
                exam.setScheduledDateTime(dateTimePicker.getValue());
                exam.setModalityEntity(modalitySelector.getValue());
                exam.setStatus(ExamStatus.PLANNED);
                examRepo.save(exam);

                refreshScheduleList();
            } else {
                if (modalitySelector.getValue() == null) {
                    Notification.show("Veuillez sélectionner un équipement", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                } else if (dateTimePicker.getValue() == null) {
                    Notification.show("Veuillez sélectionner une date et heure de début", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                } else {
                    Notification.show("Veuillez sélectionner une date et heure de fin", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        });

        dialog.open();
    }

    private void filterGrid() {
        // Récupérer l'hôpital de l'utilisateur connecté
        Hospital userHospital = getCurrentUserHospital();

        // Récupérer les examens à planifier (status CREATED et PLANNED) filtrés par hôpital
        List<Exam> examsToSchedule;
        if (userHospital != null) {
            List<Exam> createdExams = examRepo.findByStatusAndHospitalId(ExamStatus.CREATED, userHospital.getId());
            List<Exam> plannedExams = examRepo.findByStatusAndHospitalId(ExamStatus.PLANNED, userHospital.getId());
            examsToSchedule = new java.util.ArrayList<>();
            examsToSchedule.addAll(createdExams);
            examsToSchedule.addAll(plannedExams);
        } else {
            examsToSchedule = List.of();
        }

        // Récupérer les créneaux planifiés (status SCHEDULED) filtrés par hôpital
        List<ScheduleSlot> scheduledSlots;
        if (userHospital != null) {
            scheduledSlots = scheduleSlotRepo.findByStatusAndHospitalIdOrderByScheduledStartTime(
                    ScheduleSlotStatus.SCHEDULED, userHospital.getId());

            // Pré-charger les techniciens pour éviter les lazy loading
            for (ScheduleSlot slot : scheduledSlots) {
                if (slot.getTechnician() != null && slot.getTechnician().getId() != null) {
                    Technician reloadedTech = technicianRepo.findById(slot.getTechnician().getId()).orElse(null);
                    if (reloadedTech != null) {
                        slot.setTechnician(reloadedTech);
                    }
                }
            }
        } else {
            scheduledSlots = List.of();
        }

        // Combiner les examens et les créneaux pour l'affichage
        List<Object> allItems = new java.util.ArrayList<>();
        allItems.addAll(examsToSchedule);
        allItems.addAll(scheduledSlots);

        // Appliquer les filtres
        String patientSearch = patientSearchField.getValue() != null ?
                patientSearchField.getValue().toLowerCase() : "";
        ModalityType selectedModality = modalityFilter.getValue();
        String selectedStatus = statusFilter.getValue();
        Technician selectedTechnician = technicianFilter.getValue();

        List<Object> filteredItems = allItems.stream()
                .filter(item -> {
                    // Filtre patient
                    if (!patientSearch.isEmpty()) {
                        String patientName = "";
                        if (item instanceof Exam) {
                            Exam exam = (Exam) item;
                            if (exam.getPatient() != null) {
                                patientName = (exam.getPatient().getLastName() + " " +
                                        exam.getPatient().getFirstName()).toLowerCase();
                            }
                        } else if (item instanceof ScheduleSlot) {
                            ScheduleSlot slot = (ScheduleSlot) item;
                            if (slot.getOrderLine() != null && slot.getOrderLine().getPatient() != null) {
                                patientName = (slot.getOrderLine().getPatient().getLastName() + " " +
                                        slot.getOrderLine().getPatient().getFirstName()).toLowerCase();
                            }
                        }
                        if (!patientName.contains(patientSearch)) {
                            return false;
                        }
                    }

                    // Filtre modalité
                    if (selectedModality != null) {
                        ModalityType itemModality = null;
                        if (item instanceof Exam) {
                            Exam exam = (Exam) item;
                            if (exam.getModalityEntity() != null && exam.getModalityEntity().getModalityType() != null) {
                                itemModality = exam.getModalityEntity().getModalityType();
                            }
                        } else if (item instanceof ScheduleSlot) {
                            ScheduleSlot slot = (ScheduleSlot) item;
                            if (slot.getModalityResource() != null && slot.getModalityResource().getModalityType() != null) {
                                itemModality = slot.getModalityResource().getModalityType();
                            }
                        }
                        if (itemModality == null || !itemModality.getId().equals(selectedModality.getId())) {
                            return false;
                        }
                    }

                    // Filtre statut
                    if (selectedStatus != null && !selectedStatus.equals("Tous")) {
                        String itemStatus = "";
                        if (item instanceof Exam) {
                            Exam exam = (Exam) item;
                            if (exam.getStatus() != null) {
                                switch (exam.getStatus()) {
                                    case CREATED:
                                        itemStatus = "Créé";
                                        break;
                                    case PLANNED:
                                        itemStatus = "Planifié";
                                        break;
                                    case IN_PROGRESS:
                                        itemStatus = "En cours";
                                        break;
                                    case COMPLETED:
                                        itemStatus = "Terminé";
                                        break;
                                    case CANCELLED:
                                        itemStatus = "Annulé";
                                        break;
                                }
                            }
                        } else if (item instanceof ScheduleSlot) {
                            ScheduleSlot slot = (ScheduleSlot) item;
                            if (slot.getStatus() != null) {
                                switch (slot.getStatus()) {
                                    case SCHEDULED:
                                        itemStatus = "Planifié";
                                        break;
                                    case IN_PROGRESS:
                                        itemStatus = "En cours";
                                        break;
                                    case COMPLETED:
                                        itemStatus = "Terminé";
                                        break;
                                    case CANCELLED:
                                        itemStatus = "Annulé";
                                        break;
                                    case NO_SHOW:
                                        itemStatus = "Absent";
                                        break;
                                    case BLOCKED:
                                        itemStatus = "Bloqué";
                                        break;
                                }
                            }
                        }
                        if (!itemStatus.equals(selectedStatus)) {
                            return false;
                        }
                    }

                    // Filtre technicien
                    if (selectedTechnician != null) {
                        Technician itemTechnician = null;
                        if (item instanceof Exam) {
                            Exam exam = (Exam) item;
                            List<ScheduleSlot> slots = scheduleSlotRepo.findByOrderLine(exam);
                            if (!slots.isEmpty() && slots.get(0).getTechnician() != null) {
                                itemTechnician = slots.get(0).getTechnician();
                            }
                        } else if (item instanceof ScheduleSlot) {
                            ScheduleSlot slot = (ScheduleSlot) item;
                            itemTechnician = slot.getTechnician();
                        }
                        if (itemTechnician == null ||
                                !itemTechnician.getId().equals(selectedTechnician.getId())) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(java.util.stream.Collectors.toList());

        // Mettre à jour le grid avec les éléments filtrés
        schedulingGrid.setItems(filteredItems);
    }

    private void clearFilters() {
        patientSearchField.clear();
        modalityFilter.clear();
        statusFilter.setValue("Tous");
        technicianFilter.clear();
        refreshScheduleList();
    }

    private void refreshScheduleList() {
        // Récupérer l'hôpital de l'utilisateur connecté
        Hospital userHospital = getCurrentUserHospital();

        // Récupérer les examens à planifier (status CREATED et PLANNED) filtrés par hôpital
        List<Exam> examsToSchedule;
        if (userHospital != null) {
            List<Exam> createdExams = examRepo.findByStatusAndHospitalId(ExamStatus.CREATED, userHospital.getId());
            List<Exam> plannedExams = examRepo.findByStatusAndHospitalId(ExamStatus.PLANNED, userHospital.getId());
            examsToSchedule = new java.util.ArrayList<>();
            examsToSchedule.addAll(createdExams);
            examsToSchedule.addAll(plannedExams);
        } else {
            // Si pas d'hôpital, retourner une liste vide
            examsToSchedule = List.of();
        }

        // Récupérer les créneaux planifiés (status SCHEDULED) filtrés par hôpital
        List<ScheduleSlot> scheduledSlots;
        if (userHospital != null) {
            scheduledSlots = scheduleSlotRepo.findByStatusAndHospitalIdOrderByScheduledStartTime(
                    ScheduleSlotStatus.SCHEDULED, userHospital.getId());

            // Pré-charger les techniciens pour éviter les lazy loading
            for (ScheduleSlot slot : scheduledSlots) {
                if (slot.getTechnician() != null && slot.getTechnician().getId() != null) {
                    Technician reloadedTech = technicianRepo.findById(slot.getTechnician().getId()).orElse(null);
                    if (reloadedTech != null) {
                        slot.setTechnician(reloadedTech);
                    }
                }
            }
        } else {
            scheduledSlots = List.of();
        }

        // Combiner les examens et les créneaux pour l'affichage
        List<Object> allItems = new java.util.ArrayList<>();
        allItems.addAll(examsToSchedule);
        allItems.addAll(scheduledSlots);

        // Mettre à jour le data provider du grid
        schedulingGrid.setItems(allItems);
    }

    private void updateSchedulingCountBadge(Span badge) {
        Hospital userHospital = getCurrentUserHospital();

        if (userHospital != null) {
            List<Exam> createdExams = examRepo.findByStatusAndHospitalId(ExamStatus.CREATED, userHospital.getId());
            List<Exam> plannedExams = examRepo.findByStatusAndHospitalId(ExamStatus.PLANNED, userHospital.getId());
            int totalCount = createdExams.size() + plannedExams.size();

            badge.setText(totalCount + " examens");
        } else {
            badge.setText("0 examens");
        }
    }

    // Méthode pour récupérer l'hôpital de l'utilisateur connecté
    @Transactional(readOnly = true)
    private Hospital getCurrentUserHospital() {
        try {
            // Essayer directement avec le contexte de sécurité
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            System.out.println("DEBUG SchedulingView: Authentication = " + auth);

            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
                User currentUser = userDetails.getUser();
                System.out.println("DEBUG SchedulingView: User from CustomUserDetails - ID: " + currentUser.getId());
                System.out.println("DEBUG SchedulingView: User hospital: " + currentUser.getHospital());
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
}