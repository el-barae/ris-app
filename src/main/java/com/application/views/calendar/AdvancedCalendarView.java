package com.application.views.calendar;

import com.application.entity.Exam;
import com.application.entity.ExamStatus;
import com.application.entity.Modality;
import com.application.entity.ModalityType;
import com.application.entity.ScheduleSlot;
import com.application.entity.ScheduleSlotStatus;
import com.application.entity.Technician;
import com.application.repository.ExamRepository;
import com.application.repository.ScheduleSlotRepository;
import com.application.repository.ModalityRepository;
import com.application.repository.ModalityTypeRepository;
import com.application.repository.TechnicianRepository;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.data.provider.ListDataProvider;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AdvancedCalendarView {

    private final ExamRepository examRepo;
    private final ScheduleSlotRepository scheduleSlotRepo;
    private final ModalityRepository modalityRepo;
    private final ModalityTypeRepository modalityTypeRepo;
    private final TechnicianRepository technicianRepo;

    private Dialog dialog;
    private DatePicker datePicker;
    private List<ScheduleSlot> allSlots;
    private List<Exam> allExams;
    private VerticalLayout weekCalendarLayout;
    private LocalDate currentWeekStart;

    // Composants de filtrage
    private ComboBox<ModalityType> modalityTypeSelector;
    private ComboBox<Modality> modalitySelector;
    private ComboBox<Technician> technicianSelector;
    private RadioButtonGroup<String> filterTypeSelector;

    // État du filtrage
    private String currentFilterType = "modality";
    private ModalityType selectedModalityType;
    private Modality selectedModality;
    private Technician selectedTechnician;

    public AdvancedCalendarView(ExamRepository examRepo, ScheduleSlotRepository scheduleSlotRepo,
                                ModalityRepository modalityRepo, ModalityTypeRepository modalityTypeRepo, TechnicianRepository technicianRepo) {
        this.examRepo = examRepo;
        this.scheduleSlotRepo = scheduleSlotRepo;
        this.modalityRepo = modalityRepo;
        this.modalityTypeRepo = modalityTypeRepo;
        this.technicianRepo = technicianRepo;
        initializeDialog();
    }

    private void initializeDialog() {
        dialog = new Dialog();
        dialog.setHeaderTitle("📅 Calendrier Avancé");

        // Boutons du header
        Button refreshBtn = new Button("Actualiser", VaadinIcon.REFRESH.create());
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshBtn.addClickListener(e -> {
            loadFilteredData();
            updateWeekCalendar();
        });

        Button closeBtn = new Button("Fermer", e -> dialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeBtn.getStyle()
                .set("margin-left", "20px")
                .set("background", "#7f1d1d !important")
                .set("color", "white !important")
                .set("border", "none !important");

        dialog.getHeader().add(refreshBtn, closeBtn);
        dialog.setWidth("98vw");
        dialog.setHeight("95vh");
        dialog.setModal(true);
        dialog.setDraggable(true);

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        // Section de filtrage
        Component filterSection = createFilterSection();

        // Barre d'outils avec navigation
        HorizontalLayout toolbar = createToolbar();

        // Calendrier hebdomadaire
        weekCalendarLayout = new VerticalLayout();
        weekCalendarLayout.setHeight("100%");
        weekCalendarLayout.setFlexGrow(1);

        layout.add(filterSection, toolbar, weekCalendarLayout);
        dialog.add(layout);

        // Initialiser les données
        initializeFilterComponents();
        loadFilteredData();
        updateWeekCalendar();
    }

    private Component createFilterSection() {
        HorizontalLayout filterLayout = new HorizontalLayout();
        filterLayout.setWidthFull();
        filterLayout.setPadding(true);
        filterLayout.setSpacing(true);
        filterLayout.setAlignItems(HorizontalLayout.Alignment.END);
        filterLayout.getStyle()
                .set("background-color", "#f8f9fa")
                .set("border", "1px solid #dee2e6")
                .set("border-radius", "8px");

        // Étape 1 : Sélecteur de type de filtrage (Par modalité / Par technicien)
        filterTypeSelector = new RadioButtonGroup<>();
        filterTypeSelector.setLabel("1. Type de filtrage");
        filterTypeSelector.setItems("modality", "technician");
        filterTypeSelector.setValue("modality");

        // Labels personnalisés pour les radio buttons
        Map<String, String> itemLabels = Map.of(
                "modality", "Par modalité",
                "technician", "Par technicien"
        );
        filterTypeSelector.setItemLabelGenerator(itemLabels::get);

        filterTypeSelector.addValueChangeListener(e -> {
            currentFilterType = e.getValue();
            updateFilterVisibility();
            clearFilters();
            loadFilteredData();
            updateWeekCalendar();
        });

        // Étape 2a : Sélecteur de type de modalité (si filtrage par modalité)
        modalityTypeSelector = new ComboBox<>("2. Type de modalité");
        modalityTypeSelector.setItems(modalityTypeRepo.findAllActiveOrdered());
        modalityTypeSelector.setItemLabelGenerator(modalityType -> {
            if (modalityType == null) return "";
            String name = modalityType.getName() != null ? modalityType.getName() : "";
            String code = modalityType.getCode() != null ? " (" + modalityType.getCode() + ")" : "";
            return name + code;
        });
        modalityTypeSelector.setPlaceholder("Sélectionner le type");
        modalityTypeSelector.setClearButtonVisible(true);
        modalityTypeSelector.setWidth("280px");
        modalityTypeSelector.setEnabled(false);

        modalityTypeSelector.addValueChangeListener(e -> {
            selectedModalityType = e.getValue();
            if (selectedModalityType != null) {
                // Mettre à jour la liste des modalités (équipements) selon le type sélectionné
                List<Modality> modalitiesForType = modalityRepo.findByModalityTypeAndIsActive(selectedModalityType, true);
                modalitySelector.setItems(modalitiesForType);
                modalitySelector.setEnabled(true);
            } else {
                modalitySelector.clear();
                modalitySelector.setEnabled(false);
            }
            selectedModality = null;
            loadFilteredData();
            updateWeekCalendar();
        });

        // Étape 2b : Sélecteur de technicien (si filtrage par technicien)
        technicianSelector = new ComboBox<>("2. Technicien");
        technicianSelector.setItems(technicianRepo.findByIsActive(true));
        technicianSelector.setItemLabelGenerator(technician -> {
            if (technician == null) return "";
            String fullName = technician.getFullName() != null ? technician.getFullName() : "";
            String employeeId = technician.getEmployeeId() != null ? " (" + technician.getEmployeeId() + ")" : "";
            return fullName + employeeId;
        });
        technicianSelector.setPlaceholder("Sélectionner le technicien");
        technicianSelector.setClearButtonVisible(true);
        technicianSelector.setWidth("250px");
        technicianSelector.setEnabled(false);

        technicianSelector.addValueChangeListener(e -> {
            selectedTechnician = e.getValue();
            loadFilteredData();
            updateWeekCalendar();
        });

        // Étape 3 : Sélecteur de modalité (équipement physique) - seulement si filtrage par modalité
        modalitySelector = new ComboBox<>("3. Équipement");
        modalitySelector.setItemLabelGenerator(modality -> {
            if (modality == null) return "";
            String name = modality.getNom() != null ? modality.getNom() : "";
            String aetitle = modality.getAetitle() != null ? " (" + modality.getAetitle() + ")" : "";
            String brand = modality.getMarque() != null ? " - " + modality.getMarque() : "";
            return name + aetitle + brand;
        });
        modalitySelector.setPlaceholder("Sélectionner l'équipement");
        modalitySelector.setClearButtonVisible(true);
        modalitySelector.setWidth("300px");
        modalitySelector.setEnabled(false);

        modalitySelector.addValueChangeListener(e -> {
            selectedModality = e.getValue();
            loadFilteredData();
            updateWeekCalendar();
        });

        // Bouton pour réinitialiser les filtres
        Button clearFiltersBtn = new Button("Réinitialiser", VaadinIcon.CLOSE_CIRCLE.create());
        clearFiltersBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        clearFiltersBtn.addClickListener(e -> {
            clearAllFilters();
            loadFilteredData();
            updateWeekCalendar();
        });

        filterLayout.add(filterTypeSelector, modalityTypeSelector, technicianSelector, modalitySelector, clearFiltersBtn);

        return filterLayout;
    }

    private void updateFilterVisibility() {
        boolean isModalityFilter = "modality".equals(currentFilterType);

        if (isModalityFilter) {
            // Filtrage par modalité : activer le sélecteur de type de modalité
            modalityTypeSelector.setEnabled(true);
            modalityTypeSelector.setVisible(true);
            technicianSelector.setEnabled(false);
            technicianSelector.setVisible(false);
            modalitySelector.setEnabled(false);
            modalitySelector.setVisible(true);
        } else {
            // Filtrage par technicien : activer le sélecteur de technicien
            modalityTypeSelector.setEnabled(false);
            modalityTypeSelector.setVisible(false);
            technicianSelector.setEnabled(true);
            technicianSelector.setVisible(true);
            modalitySelector.setEnabled(false);
            modalitySelector.setVisible(false);
        }
    }

    private void clearFilters() {
        selectedModalityType = null;
        selectedModality = null;
        selectedTechnician = null;

        if ("modality".equals(currentFilterType)) {
            modalityTypeSelector.clear();
            modalitySelector.clear();
        } else {
            technicianSelector.clear();
        }
    }

    private void clearAllFilters() {
        selectedModalityType = null;
        selectedModality = null;
        selectedTechnician = null;

        modalityTypeSelector.clear();
        modalitySelector.clear();
        technicianSelector.clear();

        // Réinitialiser l'état des composants selon le type de filtrage actuel
        updateFilterVisibility();
        
        // Désactiver les sélecteurs qui dépendent d'une sélection
        if ("modality".equals(currentFilterType)) {
            modalitySelector.setEnabled(false);
        }
    }

    private HorizontalLayout createToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setAlignItems(HorizontalLayout.Alignment.CENTER);

        Button previousWeekBtn = new Button("◀", e -> navigateWeek(-1));
        previousWeekBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        datePicker = new DatePicker("Semaine du");
        datePicker.setValue(LocalDate.now());
        datePicker.addValueChangeListener(e -> updateWeekCalendar());

        Button nextWeekBtn = new Button("▶", e -> navigateWeek(1));
        nextWeekBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button todayBtn = new Button("Aujourd'hui", VaadinIcon.CALENDAR.create());
        todayBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        todayBtn.getStyle()
                .set("background", "#7f1d1d !important")
                .set("color", "white !important")
                .set("border", "none !important");
        todayBtn.addClickListener(e -> {
            datePicker.setValue(LocalDate.now());
            updateWeekCalendar();
        });

        toolbar.add(previousWeekBtn, datePicker, nextWeekBtn, todayBtn);
        toolbar.setFlexGrow(1, datePicker);

        return toolbar;
    }

    private void initializeFilterComponents() {
        // Initialiser l'état des composants selon le type de filtrage par défaut
        currentFilterType = "modality";
        updateFilterVisibility();
    }

    private void loadFilteredData() {
        try {
            if ("modality".equals(currentFilterType) && selectedModality != null) {
                // Filtrage par modalité
                allSlots = scheduleSlotRepo.findByModalityResourceAndStatusOrderByScheduledStartTime(
                        selectedModality, ScheduleSlotStatus.SCHEDULED);
                allExams = examRepo.findByStatusWithRelations(com.application.entity.ExamStatus.PLANNED).stream()
                        .filter(exam -> exam.getModalityEntity() != null &&
                                exam.getModalityEntity().getId().equals(selectedModality.getId()))
                        .collect(Collectors.toList());
            } else if ("technician".equals(currentFilterType) && selectedTechnician != null) {
                // Filtrage par technicien - utiliser la requête personnalisée
                System.out.println("DEBUG: Technician selected - ID: " + selectedTechnician.getId() + ", Name: " + selectedTechnician.getFullName());
                
                // Essayer d'abord avec la méthode simple
                allSlots = scheduleSlotRepo.findByTechnicianIdAndStatus(selectedTechnician.getId(), ScheduleSlotStatus.SCHEDULED);
                System.out.println("DEBUG: Simple method found " + allSlots.size() + " slots");
                
                // Si la méthode simple fonctionne, essayer avec la méthode complète
                if (allSlots.size() > 0) {
                    allSlots = scheduleSlotRepo.findByTechnicianIdAndStatusOrderByScheduledStartTime(
                            selectedTechnician.getId(), ScheduleSlotStatus.SCHEDULED);
                    System.out.println("DEBUG: Full method found " + allSlots.size() + " slots");
                }
                
                System.out.println("DEBUG: Final result - Found " + allSlots.size() + " slots for technician " + selectedTechnician.getFullName());
                
                // Récupérer les examens correspondants aux créneaux du technicien
                allExams = allSlots.stream()
                        .filter(slot -> slot.getOrderLine() != null)
                        .map(ScheduleSlot::getOrderLine)
                        .filter(exam -> exam.getStatus() == ExamStatus.PLANNED)
                        .distinct()
                        .collect(Collectors.toList());
                
                System.out.println("DEBUG: Found " + allExams.size() + " exams for technician " + selectedTechnician.getFullName());
            } else {
                // Pas de filtrage
                allSlots = new ArrayList<>();
                allExams = new ArrayList<>();
            }
        } catch (Exception e) {
            Notification.show("Erreur lors du chargement des données: " + e.getMessage(),
                            3000, Notification.Position.BOTTOM_END)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            allSlots = new ArrayList<>();
            allExams = new ArrayList<>();
        }
    }

    private void updateWeekCalendar() {
        LocalDate selectedDate = datePicker.getValue();
        if (selectedDate == null) return;

        // Calculer le début de la semaine (lundi)
        currentWeekStart = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        weekCalendarLayout.removeAll();

        // Créer l'en-tête de la semaine
        HorizontalLayout weekHeader = createWeekHeader();
        weekCalendarLayout.add(weekHeader);

        // Créer les jours de la semaine
        HorizontalLayout weekDays = new HorizontalLayout();
        weekDays.setWidthFull();
        weekDays.setSpacing(false);

        for (int i = 0; i < 7; i++) {
            LocalDate dayDate = currentWeekStart.plusDays(i);
            VerticalLayout dayLayout = createDayLayout(dayDate);
            weekDays.add(dayLayout);
            weekDays.setFlexGrow(1, dayLayout);
        }

        weekCalendarLayout.add(weekDays);
        weekCalendarLayout.setFlexGrow(1, weekDays);
    }

    private HorizontalLayout createWeekHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setSpacing(false);
        header.getStyle().set("background-color", "#007bff").set("color", "white").set("padding", "10px");

        String[] days = {"Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"};

        for (int i = 0; i < 7; i++) {
            LocalDate dayDate = currentWeekStart.plusDays(i);
            Span daySpan = new Span(days[i] + " " + dayDate.getDayOfMonth());
            daySpan.getStyle().set("font-weight", "bold").set("text-align", "center").set("flex-grow", "1");
            header.add(daySpan);
        }

        return header;
    }

    private VerticalLayout createDayLayout(LocalDate dayDate) {
        VerticalLayout dayLayout = new VerticalLayout();
        dayLayout.setWidthFull();
        dayLayout.setSpacing(false);
        dayLayout.getStyle()
                .set("border", "1px solid #dee2e6")
                .set("min-height", "400px")
                .set("background-color", "#ffffff");

        // En-tête du jour
        Span dayHeader = new Span(dayDate.format(DateTimeFormatter.ofPattern("dd MMM")));
        dayHeader.getStyle()
                .set("background-color", "#f8f9fa")
                .set("padding", "5px")
                .set("font-weight", "bold")
                .set("text-align", "center")
                .set("width", "100%")
                .set("border-bottom", "1px solid #dee2e6");
        dayLayout.add(dayHeader);

        // Ajouter les créneaux et examens du jour
        LocalDateTime dayStart = dayDate.atStartOfDay();
        LocalDateTime dayEnd = dayDate.atTime(LocalTime.MAX);

        // Ajouter les créneaux horaires
        for (ScheduleSlot slot : allSlots) {
            if (isWithinDay(slot.getScheduledStartTime(), dayStart, dayEnd)) {
                dayLayout.add(createSlotComponent(slot));
            }
        }

        // Ajouter les examens
        for (Exam exam : allExams) {
            if (exam.getScheduledDateTime() != null &&
                    isWithinDay(exam.getScheduledDateTime(), dayStart, dayEnd)) {
                dayLayout.add(createExamComponent(exam));
            }
        }

        return dayLayout;
    }

    private Component createSlotComponent(ScheduleSlot slot) {
        HorizontalLayout slotLayout = new HorizontalLayout();
        slotLayout.setWidthFull();
        slotLayout.setSpacing(false);
        slotLayout.getStyle()
                .set("background-color", "#d4edda")
                .set("border", "1px solid #c3e6cb")
                .set("border-radius", "4px")
                .set("margin", "2px")
                .set("padding", "4px");

        Span timeSpan = new Span(slot.getScheduledStartTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeSpan.getStyle().set("font-weight", "bold").set("font-size", "12px");

        Span modalitySpan = new Span(slot.getModalityResource().getNom());
        modalitySpan.getStyle().set("font-size", "11px");

        Span technicianSpan = new Span("");
        if (slot.getTechnician() != null) {
            technicianSpan.setText("👨‍⚕️ " + slot.getTechnician().getFullName());
            technicianSpan.getStyle().set("font-size", "10px").set("color", "#6c757d");
        }

        VerticalLayout content = new VerticalLayout(timeSpan, modalitySpan, technicianSpan);
        content.setSpacing(false);
        content.setPadding(false);
        content.setMargin(false);

        slotLayout.add(content);
        return slotLayout;
    }

    private Component createExamComponent(Exam exam) {
        HorizontalLayout examLayout = new HorizontalLayout();
        examLayout.setWidthFull();
        examLayout.setSpacing(false);
        examLayout.getStyle()
                .set("background-color", "#fff3cd")
                .set("border", "1px solid #ffeaa7")
                .set("border-radius", "4px")
                .set("margin", "2px")
                .set("padding", "4px");

        Span timeSpan = new Span(exam.getScheduledDateTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        timeSpan.getStyle().set("font-weight", "bold").set("font-size", "12px");

        Span patientSpan = new Span("👤 " + exam.getPatient().getLastName() + " " + exam.getPatient().getFirstName());
        patientSpan.getStyle().set("font-size", "11px");

        Span modalitySpan = new Span(exam.getModalityEntity().getNom());
        modalitySpan.getStyle().set("font-size", "10px").set("color", "#856404");

        // Ajouter le technicien associé à l'examen
        Span technicianSpan = new Span("");
        try {
            List<ScheduleSlot> slots = scheduleSlotRepo.findByOrderLine(exam);
            if (!slots.isEmpty() && slots.get(0).getTechnician() != null) {
                Technician tech = slots.get(0).getTechnician();
                if (tech.getId() != null) {
                    // Recharger le technicien pour éviter les problèmes de lazy loading
                    Technician reloadedTech = technicianRepo.findById(tech.getId()).orElse(null);
                    if (reloadedTech != null) {
                        technicianSpan.setText("👨‍⚕️ " + reloadedTech.getFullName());
                        technicianSpan.getStyle().set("font-size", "10px").set("color", "#6c757d");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération du technicien pour l'examen: " + e.getMessage());
        }

        VerticalLayout content = new VerticalLayout(timeSpan, patientSpan, modalitySpan, technicianSpan);
        content.setSpacing(false);
        content.setPadding(false);
        content.setMargin(false);

        examLayout.add(content);
        return examLayout;
    }

    private boolean isWithinDay(LocalDateTime dateTime, LocalDateTime dayStart, LocalDateTime dayEnd) {
        return !dateTime.isBefore(dayStart) && !dateTime.isAfter(dayEnd);
    }

    private void navigateWeek(int weeks) {
        LocalDate newDate = datePicker.getValue().plusWeeks(weeks);
        datePicker.setValue(newDate);
        updateWeekCalendar();
    }

    public void open() {
        dialog.open();
    }

    public void close() {
        dialog.close();
    }
}