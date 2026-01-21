package com.application.views.calendar;

import com.application.entity.Exam;
import com.application.entity.ExamStatus;
import com.application.repository.ExamRepository;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.server.VaadinSession;

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

public class ExamCalendarView {

    private final ExamRepository examRepo;
    private Dialog dialog;
    private DatePicker datePicker;
    private List<Exam> allExams;
    private VerticalLayout weekCalendarLayout;
    private LocalDate currentWeekStart;

    public ExamCalendarView(ExamRepository examRepo) {
        this.examRepo = examRepo;
        initializeDialog();
    }

    private void initializeDialog() {
        dialog = new Dialog();
        dialog.setHeaderTitle("📅 Calendrier des Examens");
        dialog.setWidth("1200px");
        dialog.setHeight("600px");
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
        todayBtn.addClickListener(e -> {
            datePicker.setValue(LocalDate.now());
            updateWeekCalendar();
        });

        toolbar.add(previousWeekBtn, datePicker, nextWeekBtn, todayBtn);
        toolbar.setFlexGrow(1, datePicker);

        // Calendrier hebdomadaire
        weekCalendarLayout = new VerticalLayout();
        weekCalendarLayout.setSpacing(false);
        weekCalendarLayout.setPadding(false);

        layout.add(toolbar, weekCalendarLayout);

        Button closeBtn = new Button("Fermer", e -> dialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(closeBtn);

        dialog.add(layout);

        // Charger les données initiales
        loadExams();
        updateWeekCalendar();
    }

    private void loadExams() {
        allExams = examRepo.findAllWithRelations();
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

        com.vaadin.flow.component.html.Span dayNameSpan = new com.vaadin.flow.component.html.Span(dayName);
        dayNameSpan.getStyle()
                .set("font-weight", "600")
                .set("font-size", "12px")
                .set("color", "#374151");

        com.vaadin.flow.component.html.Span dateSpan = new com.vaadin.flow.component.html.Span(
                dayDate.format(DateTimeFormatter.ofPattern("dd/MM")));
        dateSpan.getStyle()
                .set("font-size", "11px")
                .set("color", "#6b7280");

        // Mettre en évidence aujourd'hui
        if (dayDate.equals(LocalDate.now())) {
            dayColumn.getStyle().set("background-color", "#dbeafe");
            dayNameSpan.getStyle().set("color", "#1e40af");
        }

        dayHeader.add(dayNameSpan, dateSpan);
        dayHeader.setFlexGrow(1, dayNameSpan);

        // Ajouter les examens du jour
        List<Exam> dayExams = allExams.stream()
                .filter(exam -> exam.getScheduledDateTime() != null)
                .filter(exam -> exam.getScheduledDateTime().toLocalDate().equals(dayDate))
                .sorted((a, b) -> a.getScheduledDateTime().compareTo(b.getScheduledDateTime()))
                .collect(Collectors.toList());

        for (Exam exam : dayExams) {
            dayColumn.add(createExamCard(exam));
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
                .set("background-color", getExamColor(exam.getModality()))
                .set("border-radius", "4px")
                .set("padding", "4px")
                .set("margin", "2px 0")
                .set("cursor", "pointer")
                .set("font-size", "10px");

        String time = exam.getScheduledDateTime().format(DateTimeFormatter.ofPattern("HH:mm"));
        String patientName = exam.getPatient() != null ? 
                exam.getPatient().getLastName() : "N/A";
        
        com.vaadin.flow.component.html.Span timeSpan = new com.vaadin.flow.component.html.Span(time);
        timeSpan.getStyle().set("font-weight", "600").set("color", "white");
        
        com.vaadin.flow.component.html.Span patientSpan = new com.vaadin.flow.component.html.Span(patientName);
        patientSpan.getStyle().set("color", "white");
        
        com.vaadin.flow.component.html.Span modalitySpan = new com.vaadin.flow.component.html.Span(exam.getModality());
        modalitySpan.getStyle().set("color", "white").set("font-size", "9px");

        card.add(timeSpan, patientSpan, modalitySpan);
        card.addClickListener(e -> showExamDetails(exam));
        
        return card;
    }

    private String getExamColor(String modality) {
        return switch (modality) {
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
        content.add(createDetailRow("🏥 Modalité", exam.getModality()));
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

    private Component createDetailRow(String label, String value) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setSpacing(true);

        com.vaadin.flow.component.html.Span labelSpan = new com.vaadin.flow.component.html.Span(label + ":");
        labelSpan.getStyle()
                .set("font-weight", "600")
                .set("color", "#64748b")
                .set("min-width", "120px");

        com.vaadin.flow.component.html.Span valueSpan = new com.vaadin.flow.component.html.Span(value);
        valueSpan.getStyle()
                .set("color", "#1e293b")
                .set("font-weight", "500");

        row.add(labelSpan, valueSpan);
        return row;
    }

    public void show() {
        // Recharger les données à chaque ouverture
        loadExams();
        updateWeekCalendar();
        dialog.open();
    }

    // Classe interne pour représenter un élément du calendrier
    public static class ExamCalendarItem {
        private final Exam exam;

        public ExamCalendarItem(Exam exam) {
            this.exam = exam;
        }

        public String getTime() {
            return exam.getScheduledDateTime() != null ?
                    exam.getScheduledDateTime().format(DateTimeFormatter.ofPattern("HH:mm")) : "N/A";
        }

        public String getPatientName() {
            return exam.getPatient() != null ?
                    exam.getPatient().getLastName() + " " + exam.getPatient().getFirstName() : "N/A";
        }

        public String getModality() {
            return exam.getModality();
        }

        public String getStatus() {
            return exam.getStatus() != null ? exam.getStatus().toString() : "N/A";
        }

        public Exam getExam() {
            return exam;
        }
    }
}
