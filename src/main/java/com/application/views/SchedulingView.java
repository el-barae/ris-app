package com.application.views;

import com.application.entity.Exam;
import com.application.entity.ExamStatus;
import com.application.entity.ScheduleSlot;
import com.application.entity.ScheduleSlotStatus;
import com.application.entity.Modality;
import com.application.entity.ModalityType;
import com.application.repository.ExamRepository;
import com.application.repository.ScheduleSlotRepository;
import com.application.repository.ModalityRepository;
import com.application.repository.ModalityTypeRepository;
import com.application.views.calendar.ExamCalendarView;
import com.application.views.calendar.ModalityCalendarView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
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
    private final Grid<Object> schedulingGrid = new Grid<>();
    private ExamCalendarView examCalendar;

    public SchedulingView(ExamRepository examRepo, ScheduleSlotRepository scheduleSlotRepo, ModalityRepository modalityRepo, ModalityTypeRepository modalityTypeRepo) {
        this.examRepo = examRepo;
        this.scheduleSlotRepo = scheduleSlotRepo;
        this.modalityRepo = modalityRepo;
        this.modalityTypeRepo = modalityTypeRepo;
        
        // Initialize calendar
        examCalendar = new ExamCalendarView(examRepo);
        
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
        header.setWidthFull();
        header.setPadding(true);
        header.setSpacing(true);
        header.setAlignItems(Alignment.CENTER);
        header.getStyle()
                .set("background", "linear-gradient(135deg, #10b981 0%, #059669 100%)")
                .set("color", "white")
                .set("box-shadow", "0 4px 6px rgba(0,0,0,0.1)")
                .set("border-radius", "0 0 16px 16px")
                .set("margin-top", "15px")
                .set("margin-left", "15px")
                .set("margin-right", "15px")
                .set("width", "calc(100% - 30px)");

        H2 title = new H2("Planification des examens");
        title.getStyle()
                .set("margin", "0")
                .set("color", "white")
                .set("font-weight", "600");

        Button refreshBtn = new Button("Actualiser", VaadinIcon.REFRESH.create());
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        refreshBtn.getStyle()
                .set("background", "rgba(255,255,255,0.2)")
                .set("color", "white");
        refreshBtn.addClickListener(e -> refreshScheduleList());

        Button calendarBtn = new Button("Calendrier", VaadinIcon.CALENDAR.create());
        calendarBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        calendarBtn.getStyle()
                .set("background", "#7f1d1d !important")
                .set("color", "white !important")
                .set("font-weight", "bold")
                .set("border", "none !important");
        calendarBtn.addClickListener(e -> showModalitySelectionDialog());

        header.add(title, refreshBtn, calendarBtn);
        header.setFlexGrow(1, title);

        return header;
    }

    private Component createContent() {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(true);
        content.setSpacing(true);

        configureGrid();
        
        content.add(schedulingGrid);
        return content;
    }

    private void configureGrid() {
        schedulingGrid.setSizeFull();
        schedulingGrid.removeAllColumns();
        schedulingGrid.addClassName("scheduling-grid");

        // Colonne Type (Exam ou Créneau)
        schedulingGrid.addColumn(item -> {
            if (item instanceof Exam) {
                Span examBadge = new Span("Examen");
                examBadge.getElement().getThemeList().add("badge");
                examBadge.getElement().getThemeList().add("secondary");
                return examBadge;
            } else if (item instanceof ScheduleSlot) {
                Span slotBadge = new Span("Créneau");
                slotBadge.getElement().getThemeList().add("badge");
                slotBadge.getElement().getThemeList().add("primary");
                return slotBadge;
            }
            return new Span("N/A");
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
                        slot.getOrderLine().getPatient().getLastName() + " " + slot.getOrderLine().getPatient().getFirstName() : "N/A";
            }
            return "N/A";
        })
                .setHeader("Patient")
                .setSortable(true)
                .setFlexGrow(1);

        // Colonne Modalité/Équipement
        schedulingGrid.addColumn(item -> {
            if (item instanceof Exam) {
                Exam exam = (Exam) item;
                return exam.getModalityEntity() != null ?
                        exam.getModalityEntity().getModalityType().getCode() + " - " + exam.getModalityEntity().getNom() : "Non assigné";
            } else if (item instanceof ScheduleSlot) {
                ScheduleSlot slot = (ScheduleSlot) item;
                return slot.getModalityResource() != null ?
                        slot.getModalityResource().getModalityType().getCode() + " - " + slot.getModalityResource().getNom() : "N/A";
            }
            return "N/A";
        })
                .setHeader("Modalité")
                .setWidth("150px")
                .setFlexGrow(0);

        // Colonne Salle (pour les créneaux)
        schedulingGrid.addColumn(item -> {
            if (item instanceof ScheduleSlot) {
                ScheduleSlot slot = (ScheduleSlot) item;
                return slot.getModalityResource() != null && slot.getModalityResource().getRoom() != null ?
                        slot.getModalityResource().getRoom().getName() : "N/A";
            }
            return "-";
        })
                .setHeader("Salle")
                .setWidth("100px")
                .setFlexGrow(0);

        // Colonne Statut
        schedulingGrid.addColumn(item -> {
            Span statusBadge = new Span();
            
            if (item instanceof Exam) {
                Exam exam = (Exam) item;
                statusBadge.setText(exam.getStatus() != null ? exam.getStatus().toString() : "N/A");
                
                if (exam.getStatus() != null) {
                    switch (exam.getStatus()) {
                        case CREATED:
                            statusBadge.getElement().getThemeList().add("secondary");
                            break;
                        case PLANNED:
                            statusBadge.getElement().getThemeList().add("primary");
                            break;
                        case SELECTED:
                            statusBadge.getElement().getThemeList().add("success");
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
            } else if (item instanceof ScheduleSlot) {
                ScheduleSlot slot = (ScheduleSlot) item;
                statusBadge.setText(slot.getStatus() != null ? slot.getStatus().toString() : "N/A");
                
                if (slot.getStatus() != null) {
                    switch (slot.getStatus()) {
                        case SCHEDULED:
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
                        case NO_SHOW:
                            statusBadge.getElement().getThemeList().add("error");
                            break;
                        case BLOCKED:
                            statusBadge.getElement().getThemeList().add("secondary");
                            break;
                    }
                }
            }
            
            statusBadge.getElement().getThemeList().add("badge");
            return statusBadge;
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

        // Colonne Actions
        schedulingGrid.addComponentColumn(this::createActionButtons)
                .setHeader("Actions")
                .setWidth("200px")
                .setFlexGrow(0);
    }

    private Component createActionButtons(Object item) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);

        Button detailsBtn = new Button(VaadinIcon.INFO_CIRCLE.create());
        detailsBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        detailsBtn.getElement().setProperty("title", "Voir les détails");
        
        if (item instanceof Exam) {
            Exam exam = (Exam) item;
            detailsBtn.addClickListener(e -> showExamDetails(exam));
            
            if (exam.getStatus() == ExamStatus.CREATED) {
                Button scheduleBtn = new Button("Planifier", VaadinIcon.CALENDAR.create());
                scheduleBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
                scheduleBtn.addClickListener(e -> openScheduleDialog(exam));
                actions.add(scheduleBtn);
            }
        } else if (item instanceof ScheduleSlot) {
            ScheduleSlot slot = (ScheduleSlot) item;
            detailsBtn.addClickListener(e -> showSlotDetails(slot));
            
            if (slot.getStatus() == ScheduleSlotStatus.SCHEDULED) {
                Button startBtn = new Button("Démarrer", VaadinIcon.PLAY.create());
                startBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
                startBtn.addClickListener(e -> startSlot(slot));
                actions.add(startBtn);

                Button cancelBtn = new Button(VaadinIcon.CLOSE.create());
                cancelBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
                cancelBtn.getElement().setProperty("title", "Annuler");
                cancelBtn.addClickListener(e -> cancelSlot(slot));
                actions.add(cancelBtn);
            }
        }
        
        actions.add(detailsBtn);
        return actions;
    }

    private void showSlotDetails(ScheduleSlot slot) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Détails du créneau");
        
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        
        content.add(new Span("Patient: " + (slot.getOrderLine() != null && slot.getOrderLine().getPatient() != null ? 
            slot.getOrderLine().getPatient().getLastName() + " " + slot.getOrderLine().getPatient().getFirstName() : "N/A")));
        content.add(new Span("IPP: " + (slot.getOrderLine() != null && slot.getOrderLine().getPatient() != null ? 
            slot.getOrderLine().getPatient().getPatientId() : "N/A")));
        content.add(new Span("Accession: " + (slot.getOrderLine() != null ? slot.getOrderLine().getAccessionNumber() : "N/A")));
        content.add(new Span("Modalité: " + (slot.getModalityResource() != null ?
            slot.getModalityResource().getModalityType().getCode() + " - " + slot.getModalityResource().getNom() : "N/A")));
        content.add(new Span("Salle: " + (slot.getModalityResource() != null && slot.getModalityResource().getRoom() != null ?
            slot.getModalityResource().getRoom().getName() : "N/A")));
        content.add(new Span("Statut: " + (slot.getStatus() != null ? slot.getStatus().toString() : "N/A")));
        content.add(new Span("Date de début: " + (slot.getScheduledStartTime() != null ?
            slot.getScheduledStartTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A")));
        content.add(new Span("Date de fin: " + (slot.getScheduledEndTime() != null ?
            slot.getScheduledEndTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A")));
        content.add(new Span("Priorité: " + (slot.getOrderLine() != null && slot.getOrderLine().getPriority() != null ? 
            slot.getOrderLine().getPriority().toString() : "NORMAL")));
        content.add(new Span("Médecin: " + (slot.getOrderLine() != null && slot.getOrderLine().getMedecin() != null ?
            slot.getOrderLine().getMedecin().getFirstName() + " " + slot.getOrderLine().getMedecin().getLastName() : "N/A")));
        
        dialog.add(content);
        dialog.setConfirmText("Fermer");
        dialog.setCancelButton((com.vaadin.flow.component.Component) null);
        
        dialog.open();
    }

    private void startSlot(ScheduleSlot slot) {
        slot.setStatus(ScheduleSlotStatus.IN_PROGRESS);
        if (slot.getOrderLine() != null) {
            slot.getOrderLine().setStatus(ExamStatus.IN_PROGRESS);
        }
        scheduleSlotRepo.save(slot);
        
        Notification.show(
            "Créneau démarré pour le patient " + 
            (slot.getOrderLine() != null && slot.getOrderLine().getPatient() != null ?
             slot.getOrderLine().getPatient().getLastName() + " " + slot.getOrderLine().getPatient().getFirstName() : "N/A"),
            3000,
            Notification.Position.BOTTOM_END
        ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        
        refreshScheduleList();
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

    private void showExamDetails(Exam exam) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Détails de l'examen");
        
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        
        content.add(new Span("Patient: " + (exam.getPatient() != null ? 
            exam.getPatient().getLastName() + " " + exam.getPatient().getFirstName() : "N/A")));
        content.add(new Span("IPP: " + (exam.getPatient() != null ? exam.getPatient().getPatientId() : "N/A")));
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
        modalitySelector.setItemLabelGenerator(modality -> 
            modality.getModalityType().getCode() + " - " + modality.getNom() + " (" + modality.getAetitle() + ")");
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
        
        dialog.add(content);
        dialog.setCancelable(true);
        dialog.setConfirmText("Planifier");
        dialog.setConfirmButtonTheme("primary");
        
        dialog.addConfirmListener(e -> {
            if (dateTimePicker.getValue() != null && modalitySelector.getValue() != null && endTimePicker.getValue() != null) {
                // Créer un ScheduleSlot
                ScheduleSlot slot = new ScheduleSlot();
                slot.setOrderLine(exam);
                slot.setModalityResource(modalitySelector.getValue());
                slot.setScheduledStartTime(dateTimePicker.getValue());
                slot.setScheduledEndTime(endTimePicker.getValue());
                slot.setStatus(ScheduleSlotStatus.SCHEDULED);
                
                scheduleSlotRepo.save(slot);
                
                // Mettre à jour l'examen
                exam.setScheduledDateTime(dateTimePicker.getValue());
                exam.setModalityEntity(modalitySelector.getValue());
                exam.setStatus(ExamStatus.PLANNED);
                examRepo.save(exam);
                
                Notification.show(
                    "Examen planifié avec succès pour le " + 
                    dateTimePicker.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) +
                    " avec l'équipement " + modalitySelector.getValue().getNom() +
                    " (" + modalitySelector.getValue().getModalityType().getCode() + ")",
                    3000,
                    Notification.Position.BOTTOM_END
                ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
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

    private void showModalitySelectionDialog() {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Sélectionner une modalité pour le calendrier");
        
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setWidth("400px");
        
        // Sélecteur de type de modalité
        ComboBox<ModalityType> modalityTypeSelector = new ComboBox<>("Type de modalité");
        modalityTypeSelector.setItems(modalityTypeRepo.findAll());
        modalityTypeSelector.setItemLabelGenerator(ModalityType::getDescription);
        modalityTypeSelector.setPlaceholder("Sélectionner un type de modalité");
        modalityTypeSelector.setWidthFull();
        content.add(modalityTypeSelector);
        
        // Sélecteur de modalité (équipement)
        ComboBox<Modality> modalitySelector = new ComboBox<>("Équipement");
        modalitySelector.setPlaceholder("D'abord sélectionner un type de modalité");
        modalitySelector.setWidthFull();
        modalitySelector.setEnabled(false);
        content.add(modalitySelector);
        
        // Mettre à jour les modalités quand le type change
        modalityTypeSelector.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                List<Modality> modalities = modalityRepo.findByModalityTypeAndIsActive(e.getValue(), true);
                modalitySelector.setItems(modalities);
                modalitySelector.setItemLabelGenerator(modality -> 
                    modality.getNom() + " (" + modality.getAetitle() + ")");
                modalitySelector.setEnabled(true);
                modalitySelector.setPlaceholder("Sélectionner un équipement");
            } else {
                modalitySelector.setItems();
                modalitySelector.setEnabled(false);
                modalitySelector.setPlaceholder("D'abord sélectionner un type de modalité");
            }
        });
        
        dialog.add(content);
        dialog.setCancelable(true);
        dialog.setConfirmText("Afficher le calendrier");
        dialog.setConfirmButtonTheme("primary");
        
        dialog.addConfirmListener(e -> {
            if (modalitySelector.getValue() != null) {
                // Fermer le dialogue de sélection
                dialog.close();
                
                // Afficher le calendrier pour la modalité sélectionnée
                showModalityCalendar(modalitySelector.getValue());
                
            } else {
                Notification.show("Veuillez sélectionner un équipement", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        dialog.open();
    }

    private void showModalityCalendar(Modality selectedModality) {
        // Filtrer les examens pour cette modalité
        List<Exam> filteredExams = examRepo.findAllWithRelations()
            .stream()
            .filter(exam -> exam.getModalityEntity() != null && 
                           exam.getModalityEntity().getId().equals(selectedModality.getId()))
            .toList();
        
        // Créer un calendrier personnalisé pour cette modalité
        ModalityCalendarView calendarView = new ModalityCalendarView(examRepo, scheduleSlotRepo, selectedModality, filteredExams);
        calendarView.show();
    }

    private void openCreateSlotDialog(Modality modality) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Créer un créneau pour " + modality.getNom());
        
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        
        // Information sur la modalité
        content.add(new Span("Équipement: " + modality.getNom() + " (" + modality.getAetitle() + ")"));
        content.add(new Span("Type: " + modality.getModalityType().getDescription()));
        
        // DateTimePicker pour la date de début
        DateTimePicker startTimePicker = new DateTimePicker("Date et heure de début");
        startTimePicker.setValue(LocalDateTime.now().plusHours(1));
        startTimePicker.setWidthFull();
        content.add(startTimePicker);
        
        // DateTimePicker pour la date de fin
        DateTimePicker endTimePicker = new DateTimePicker("Date et heure de fin");
        endTimePicker.setValue(startTimePicker.getValue().plusHours(1));
        endTimePicker.setWidthFull();
        content.add(endTimePicker);
        
        // Mettre à jour automatiquement la date de fin
        startTimePicker.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                endTimePicker.setValue(e.getValue().plusHours(1));
            }
        });
        
        dialog.add(content);
        dialog.setCancelable(true);
        dialog.setConfirmText("Créer");
        dialog.setConfirmButtonTheme("primary");
        
        dialog.addConfirmListener(e -> {
            if (startTimePicker.getValue() != null && endTimePicker.getValue() != null) {
                // Vérifier les conflits
                List<ScheduleSlot> conflictingSlots = scheduleSlotRepo.findConflictingSlots(
                    modality, startTimePicker.getValue(), endTimePicker.getValue());
                
                if (!conflictingSlots.isEmpty()) {
                    Notification.show("Conflit avec un créneau existant", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                } else {
                    // Créer le créneau sans examen
                    ScheduleSlot slot = new ScheduleSlot();
                    slot.setModalityResource(modality);
                    slot.setScheduledStartTime(startTimePicker.getValue());
                    slot.setScheduledEndTime(endTimePicker.getValue());
                    slot.setStatus(ScheduleSlotStatus.SCHEDULED);
                    // orderLine reste null pour un créneau vide
                    
                    scheduleSlotRepo.save(slot);
                    
                    Notification.show("Créneau créé avec succès", 3000, Notification.Position.BOTTOM_END)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    
                    // Réafficher le calendrier
                    showModalityCalendar(modality);
                }
            } else {
                Notification.show("Veuillez remplir toutes les dates", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        
        dialog.open();
    }

    private void openAssignExamDialog(Modality modality) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Affecter un examen à " + modality.getNom());
        
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        
        // Information sur la modalité
        content.add(new Span("Équipement: " + modality.getNom() + " (" + modality.getAetitle() + ")"));
        content.add(new Span("Type: " + modality.getModalityType().getDescription()));
        
        // Récupérer les examens non planifiés du même type
        List<Exam> availableExams = examRepo.findByStatusWithRelations(ExamStatus.CREATED)
            .stream()
            .filter(exam -> exam.getModalityEntity() != null && 
                           exam.getModalityEntity().getModalityType().equals(modality.getModalityType()))
            .toList();
        
        if (availableExams.isEmpty()) {
            content.add(new Span("Aucun examen disponible pour ce type de modalité"));
        } else {
            // Sélecteur d'examen
            ComboBox<Exam> examSelector = new ComboBox<>("Examen à affecter");
            examSelector.setItems(availableExams);
            examSelector.setItemLabelGenerator(exam -> 
                exam.getPatient().getLastName() + " " + exam.getPatient().getFirstName() + 
                " - " + exam.getAccessionNumber() + 
                " (" + exam.getModalityEntity().getModalityType().getCode() + ")");
            examSelector.setPlaceholder("Sélectionner un examen");
            examSelector.setWidthFull();
            content.add(examSelector);
            
            // DateTimePicker pour la date de début
            DateTimePicker startTimePicker = new DateTimePicker("Date et heure de début");
            startTimePicker.setValue(LocalDateTime.now().plusHours(1));
            startTimePicker.setWidthFull();
            content.add(startTimePicker);
            
            // DateTimePicker pour la date de fin
            DateTimePicker endTimePicker = new DateTimePicker("Date et heure de fin");
            endTimePicker.setValue(startTimePicker.getValue().plusHours(1));
            endTimePicker.setWidthFull();
            content.add(endTimePicker);
            
            // Mettre à jour automatiquement la date de fin
            startTimePicker.addValueChangeListener(e -> {
                if (e.getValue() != null) {
                    endTimePicker.setValue(e.getValue().plusHours(1));
                }
            });
            
            dialog.addConfirmListener(e -> {
                if (examSelector.getValue() != null && startTimePicker.getValue() != null && endTimePicker.getValue() != null) {
                    // Vérifier les conflits
                    List<ScheduleSlot> conflictingSlots = scheduleSlotRepo.findConflictingSlots(
                        modality, startTimePicker.getValue(), endTimePicker.getValue());
                    
                    if (!conflictingSlots.isEmpty()) {
                        Notification.show("Conflit avec un créneau existant", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    } else {
                        // Créer le créneau avec l'examen
                        ScheduleSlot slot = new ScheduleSlot();
                        slot.setOrderLine(examSelector.getValue());
                        slot.setModalityResource(modality);
                        slot.setScheduledStartTime(startTimePicker.getValue());
                        slot.setScheduledEndTime(endTimePicker.getValue());
                        slot.setStatus(ScheduleSlotStatus.SCHEDULED);
                        
                        scheduleSlotRepo.save(slot);
                        
                        // Mettre à jour l'examen
                        Exam exam = examSelector.getValue();
                        exam.setScheduledDateTime(startTimePicker.getValue());
                        exam.setModalityEntity(modality);
                        exam.setStatus(ExamStatus.PLANNED);
                        examRepo.save(exam);
                        
                        Notification.show("Examen affecté avec succès", 3000, Notification.Position.BOTTOM_END)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                        
                        // Réafficher le calendrier
                        showModalityCalendar(modality);
                    }
                } else {
                    if (examSelector.getValue() == null) {
                        Notification.show("Veuillez sélectionner un examen", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    } else {
                        Notification.show("Veuillez remplir toutes les dates", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                    }
                }
            });
        }
        
        dialog.add(content);
        dialog.setCancelable(true);
        dialog.setConfirmText(availableExams.isEmpty() ? "Fermer" : "Affecter");
        dialog.setConfirmButtonTheme(availableExams.isEmpty() ? "secondary" : "primary");
        
        dialog.open();
    }

    private void refreshScheduleList() {
        // Récupérer les examens à planifier (status CREATED)
        List<Exam> examsToSchedule = examRepo.findByStatusWithRelations(ExamStatus.CREATED);
        
        // Récupérer les créneaux planifiés (status SCHEDULED)
        List<ScheduleSlot> scheduledSlots = scheduleSlotRepo.findByStatusOrderByScheduledStartTime(ScheduleSlotStatus.SCHEDULED);
        
        // Combiner les deux listes
        List<Object> combinedList = new java.util.ArrayList<>();
        combinedList.addAll(examsToSchedule);
        combinedList.addAll(scheduledSlots);
        
        schedulingGrid.setItems(combinedList);
    }
}
