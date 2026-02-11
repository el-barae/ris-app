package com.application.views.calendar;

import com.application.entity.Exam;
import com.application.entity.Modality;
import com.application.entity.ScheduleSlot;
import com.application.entity.ScheduleSlotStatus;
import com.application.repository.ExamRepository;
import com.application.repository.ScheduleSlotRepository;
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
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
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

public class ModalityCalendarView {

    private final ExamRepository examRepo;
    private final ScheduleSlotRepository scheduleSlotRepo;
    private final Modality selectedModality;
    private final List<Exam> filteredExams;
    private Dialog dialog;
    private DatePicker datePicker;
    private List<ScheduleSlot> allSlots;
    private VerticalLayout weekCalendarLayout;
    private LocalDate currentWeekStart;
    private Runnable onModalityChangeCallback;

    public ModalityCalendarView(ExamRepository examRepo, ScheduleSlotRepository scheduleSlotRepo, 
                                Modality selectedModality, List<Exam> filteredExams) {
        this.examRepo = examRepo;
        this.scheduleSlotRepo = scheduleSlotRepo;
        this.selectedModality = selectedModality;
        this.filteredExams = filteredExams;
        initializeDialog();
    }
    
    public void setOnModalityChangeCallback(Runnable callback) {
        this.onModalityChangeCallback = callback;
    }

    private void initializeDialog() {
        dialog = new Dialog();
        dialog.setHeaderTitle("📅 Calendrier - " + selectedModality.getNom() + 
                              " (" + selectedModality.getModalityType().getCode() + ")");
        
        // Ajouter les boutons dans le header
        Button changeModalityBtn = new Button("Changer de modalité", VaadinIcon.EXCHANGE.create());
        changeModalityBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        changeModalityBtn.addClickListener(e -> {
            dialog.close();
            // Exécuter le callback pour réouvrir la sélection de modalité
            if (onModalityChangeCallback != null) {
                onModalityChangeCallback.run();
            }
        });
        
        Button refreshBtn = new Button("Actualiser", VaadinIcon.REFRESH.create());
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshBtn.addClickListener(e -> {
            loadSlots();
            updateWeekCalendar();
        });
        
        Button closeBtn = new Button("Fermer", e -> dialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeBtn.getStyle().set("margin-left", "20px").set("background-color", "red").set("color", "white");
        
        dialog.getHeader().add(changeModalityBtn, refreshBtn, closeBtn);
        dialog.setWidth("98vw");
        dialog.setHeight("95vh");
        dialog.setModal(true);
        dialog.setDraggable(true);

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(true);
        layout.setSpacing(true);

        // Barre d'outils avec sélecteur de date et navigation
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
        todayBtn.getStyle().set("background-color", "darkred").set("color", "white");
        todayBtn.addClickListener(e -> {
            datePicker.setValue(LocalDate.now());
            updateWeekCalendar();
        });

        toolbar.add(previousWeekBtn, datePicker, nextWeekBtn, todayBtn);
        toolbar.setFlexGrow(1, datePicker);

        // Calendrier hebdomadaire
        weekCalendarLayout = new VerticalLayout();
        weekCalendarLayout.setHeight("100%");
        weekCalendarLayout.setFlexGrow(1);

        layout.setSizeFull();
        layout.setFlexGrow(1, weekCalendarLayout);
        
        layout.add(toolbar, weekCalendarLayout);
        dialog.add(layout);

        // Charger les données initiales
        loadSlots();
        updateWeekCalendar();
    }

    private void loadSlots() {
        allSlots = scheduleSlotRepo.findByModalityResourceAndStatusOrderByScheduledStartTime(
            selectedModality, ScheduleSlotStatus.SCHEDULED);
    }

    private void updateWeekCalendar() {
        LocalDate selectedDate = datePicker.getValue();
        if (selectedDate == null) return;

        // Calculer le début de la semaine (lundi)
        currentWeekStart = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        
        // Créer le calendrier hebdomadaire
        createWeekCalendar();
    }

    private void navigateWeek(int direction) {
        if (currentWeekStart == null) {
            currentWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }
        currentWeekStart = currentWeekStart.plusWeeks(direction);
        datePicker.setValue(currentWeekStart);
        updateWeekCalendar();
    }

    private void createWeekCalendar() {
        weekCalendarLayout.removeAll();
        
        // Créer l'en-tête des jours de la semaine
        HorizontalLayout weekHeader = new HorizontalLayout();
        weekHeader.setWidthFull();
        weekHeader.setSpacing(false);
        
        String[] daysOfWeek = {"Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"};
        String[] dayColors = {"#f3f4f6", "#ffffff", "#f3f4f6", "#ffffff", "#f3f4f6", "#ffffff", "#f3f4f6"};
        
        for (int i = 0; i < 7; i++) {
            LocalDate dayDate = currentWeekStart.plusDays(i);
            VerticalLayout dayColumn = createDayColumn(daysOfWeek[i], dayDate, dayColors[i]);
            dayColumn.setWidth("14.28%");
            weekHeader.add(dayColumn);
        }
        
        weekCalendarLayout.add(weekHeader);
    }

    private VerticalLayout createDayColumn(String dayName, LocalDate dayDate, String bgColor) {
        VerticalLayout dayColumn = new VerticalLayout();
        dayColumn.setSpacing(false);
        dayColumn.setPadding(true);
        dayColumn.getStyle()
                .set("background-color", bgColor)
                .set("border", "1px solid #e5e7eb")
                .set("min-height", "150px");

        // En-tête du jour
        HorizontalLayout dayHeader = new HorizontalLayout();
        dayHeader.setWidthFull();
        dayHeader.setSpacing(false);
        dayHeader.setAlignItems(HorizontalLayout.Alignment.CENTER);

        Span dayNameSpan = new Span(dayName);
        dayNameSpan.getStyle()
                .set("font-weight", "600")
                .set("font-size", "14px")
                .set("color", "#374151");

        Span dateSpan = new Span(
                dayDate.format(DateTimeFormatter.ofPattern("dd/MM")));
        dateSpan.getStyle()
                .set("font-size", "13px")
                .set("color", "#6b7280");

        // Mettre en évidence aujourd'hui
        if (dayDate.equals(LocalDate.now())) {
            dayColumn.getStyle().set("background-color", "#dbeafe");
            dayNameSpan.getStyle().set("color", "#1e40af");
        }

        dayHeader.add(dayNameSpan, dateSpan);
        dayHeader.setFlexGrow(1, dayNameSpan);

        // Ajouter les examens du jour
        List<Exam> dayExams = filteredExams.stream()
                .filter(exam -> exam.getScheduledDateTime() != null)
                .filter(exam -> exam.getScheduledDateTime().toLocalDate().equals(dayDate))
                .sorted((a, b) -> a.getScheduledDateTime().compareTo(b.getScheduledDateTime()))
                .collect(Collectors.toList());

        // Ajouter les créneaux du jour
        List<ScheduleSlot> daySlots = allSlots.stream()
                .filter(slot -> slot.getScheduledStartTime() != null)
                .filter(slot -> slot.getScheduledStartTime().toLocalDate().equals(dayDate))
                .sorted((a, b) -> a.getScheduledStartTime().compareTo(b.getScheduledStartTime()))
                .collect(Collectors.toList());

        for (Exam exam : dayExams) {
            dayColumn.add(createExamCard(exam));
        }

        for (ScheduleSlot slot : daySlots) {
            dayColumn.add(createSlotCard(slot));
        }

        dayColumn.add(dayHeader);
        return dayColumn;
    }

    private Component createExamCard(Exam exam) {
        VerticalLayout card = new VerticalLayout();
        card.setSpacing(false);
        card.setPadding(false);
        card.setMargin(false);
        card.getStyle()
                .set("background-color", getExamColor(exam.getModalityEntity().getModalityType().getCode()))
                .set("border-radius", "4px")
                .set("padding", "4px")
                .set("margin", "2px 0")
                .set("cursor", "pointer")
                .set("font-size", "12px");

        String time = exam.getScheduledDateTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        String patientName = exam.getPatient() != null ? 
                exam.getPatient().getLastName() : "N/A";
        
        Span timeSpan = new Span(time);
        timeSpan.getStyle().set("font-weight", "600").set("color", "white");
        
        Span patientSpan = new Span(patientName);
        patientSpan.getStyle().set("color", "white");
        
        Span modalitySpan = new Span(exam.getModalityEntity().getModalityType().getCode());
        modalitySpan.getStyle().set("color", "white").set("font-size", "11px");

        card.add(timeSpan, patientSpan, modalitySpan);
        card.addClickListener(e -> showExamDetails(exam));
        
        return card;
    }

    private Component createSlotCard(ScheduleSlot slot) {
        VerticalLayout card = new VerticalLayout();
        card.setSpacing(false);
        card.setPadding(false);
        card.setMargin(false);
        
        String bgColor = slot.getOrderLine() != null ? "#10b981" : "#f59e0b"; // Vert si assigné, orange si disponible
        card.getStyle()
                .set("background-color", bgColor)
                .set("border-radius", "4px")
                .set("padding", "4px")
                .set("margin", "2px 0")
                .set("cursor", "pointer")
                .set("font-size", "12px");

        String time = slot.getScheduledStartTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        String title = slot.getOrderLine() != null ? 
                (slot.getOrderLine().getPatient() != null ? 
                    slot.getOrderLine().getPatient().getLastName() : "Patient") : "Créneau disponible";
        
        Span timeSpan = new Span(time);
        timeSpan.getStyle().set("font-weight", "600").set("color", "white");
        
        Span titleSpan = new Span(title);
        titleSpan.getStyle().set("color", "white");
        
        card.add(timeSpan, titleSpan);
        card.addClickListener(e -> showSlotDetails(slot));
        
        return card;
    }

    private String getExamColor(String modalityCode) {
        return switch (modalityCode) {
            case "CT" -> "#3b82f6";  // Bleu
            case "MR" -> "#8b5cf6";  // Violet
            case "US" -> "#10b981";  // Vert
            case "CR", "XR" -> "#f59e0b";  // Orange
            default -> "#6b7280";  // Gris
        };
    }

    private void showExamDetails(Exam exam) {
        Dialog detailsDialog = new Dialog();
        detailsDialog.setHeaderTitle("📋 Détails de l'examen");
        detailsDialog.setWidth("500px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);

        content.add(createDetailRow("👤 Patient", exam.getPatient() != null ?
                exam.getPatient().getLastName() + " " + exam.getPatient().getFirstName() : "N/A"));
        content.add(createDetailRow("🆔 IPP", exam.getPatient() != null ? exam.getPatient().getPatientId() : "N/A"));
        content.add(createDetailRow("📋 Accession", exam.getAccessionNumber()));
        content.add(createDetailRow("🏥 Modalité", exam.getModalityEntity().getModalityType().getCode()));
        content.add(createDetailRow("📅 Programmé", exam.getScheduledDateTime() != null ?
                exam.getScheduledDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A"));
        content.add(createDetailRow("⚡ Statut", exam.getStatus() != null ? exam.getStatus().toString() : "N/A"));
        content.add(createDetailRow("👨‍⚕️ Médecin", exam.getMedecin() != null ?
                exam.getMedecin().getFirstName() + " " + exam.getMedecin().getLastName() : "N/A"));

        detailsDialog.add(content);

        Button closeBtn = new Button("Fermer", e -> detailsDialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        detailsDialog.getFooter().add(closeBtn);

        detailsDialog.open();
    }

    private void showSlotDetails(ScheduleSlot slot) {
        Dialog detailsDialog = new Dialog();
        detailsDialog.setHeaderTitle("📅 Détails du créneau");
        detailsDialog.setWidth("500px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);

        content.add(createDetailRow("🏥 Équipement", selectedModality.getNom()));
        content.add(createDetailRow("📍 Salle", selectedModality.getRoom() != null ? 
                selectedModality.getRoom().getName() : "N/A"));
        content.add(createDetailRow("📅 Début", slot.getScheduledStartTime() != null ?
                slot.getScheduledStartTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A"));
        content.add(createDetailRow("📅 Fin", slot.getScheduledEndTime() != null ?
                slot.getScheduledEndTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A"));
        content.add(createDetailRow("⚡ Statut", slot.getStatus() != null ? slot.getStatus().toString() : "N/A"));
        
        if (slot.getOrderLine() != null) {
            content.add(createDetailRow("👤 Patient", slot.getOrderLine().getPatient() != null ?
                    slot.getOrderLine().getPatient().getLastName() + " " + slot.getOrderLine().getPatient().getFirstName() : "N/A"));
            content.add(createDetailRow("📋 Accession", slot.getOrderLine().getAccessionNumber()));
        } else {
            content.add(createDetailRow("👤 Patient", "Créneau disponible"));
            content.add(createDetailRow("📋 Accession", "-"));
        }

        detailsDialog.add(content);

        Button closeBtn = new Button("Fermer", e -> detailsDialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        detailsDialog.getFooter().add(closeBtn);

        detailsDialog.open();
    }

    private Component createDetailRow(String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(true);

        Span labelSpan = new Span(label + ":");
        labelSpan.getStyle()
                .set("font-weight", "600")
                .set("color", "#64748b")
                .set("min-width", "120px");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("color", "#1e293b")
                .set("font-weight", "500");

        row.add(labelSpan, valueSpan);
        return row;
    }

    private void openCreateSlotDialog() {
        // Implémenter la création de créneau ici
        Notification.show("Fonctionnalité de création à implémenter", 3000, Notification.Position.MIDDLE)
            .addThemeVariants(NotificationVariant.LUMO_PRIMARY);
    }

    public void show() {
        // Recharger les données à chaque ouverture
        loadSlots();
        updateWeekCalendar();
        dialog.open();
    }
}
