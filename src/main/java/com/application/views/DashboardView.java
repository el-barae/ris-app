package com.application.views;

import com.application.entity.Exam;
import com.application.entity.ExamStatus;
import com.application.entity.UserRole;
import com.application.security.SecurityUtils;
import com.application.service.ExamService;
import com.application.service.PatientService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import jakarta.annotation.PostConstruct;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "dashboard", layout = MainLayout.class)
@PageTitle("Tableau de bord")
@RolesAllowed({"ADMIN", "MEDECIN", "RADIOLOGUE", "TECHNICIEN", "SECRETAIRE"})
public class DashboardView extends VerticalLayout {

    private final ExamService examService;
    private final PatientService patientService;

    // Statistiques
    private int totalPatients;
    private int todayExams;
    private int pendingExams;
    private int monthCompletedExams;
    private List<Exam> recentExams;

    public DashboardView(ExamService examService, PatientService patientService) {
        this.examService = examService;
        this.patientService = patientService;
    }

    @PostConstruct
    public void init() {
        loadStatistics();
        createDashboard();
    }

    private void loadStatistics() {
        try {
            totalPatients = patientService.findAll().size();
            
            LocalDate today = LocalDate.now();
            recentExams = examService.findScheduledExams(today);
            todayExams = (int) recentExams.stream()
                    .filter(exam -> exam.getScheduledDateTime().toLocalDate().equals(today))
                    .count();
            
            pendingExams = (int) examService.findByStatus(ExamStatus.PLANNED).stream()
                    .filter(exam -> exam.getScheduledDateTime().isAfter(LocalDateTime.now()))
                    .count();
            
            // Exams complétés ce mois
            LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
            monthCompletedExams = (int) examService.findByStatus(ExamStatus.COMPLETED).stream()
                    .filter(exam -> exam.getPerformedDateTime() != null && 
                            exam.getPerformedDateTime().isAfter(monthStart))
                    .count();
                    
        } catch (Exception e) {
            // Valeurs par défaut en cas d'erreur
            totalPatients = 0;
            todayExams = 0;
            pendingExams = 0;
            monthCompletedExams = 0;
            recentExams = List.of();
        }
    }

    private void createDashboard() {
        addClassName("dashboard-view");
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Titre
        H2 title = new H2();
        title.add(VaadinIcon.DASHBOARD.create());
        title.add(" Tableau de bord");
        title.addClassNames("view-header", "mb-l", "text-xl");

        add(title);

        // Cartes statistiques
        add(createStatisticsCards());

        // Boutons d'accès rapide
        add(createQuickActions());

        // Section examens récents
        add(createRecentExamsSection());
    }

    private Component createStatisticsCards() {
        HorizontalLayout cardsLayout = new HorizontalLayout();
        cardsLayout.setWidthFull();
        cardsLayout.addClassNames("gap-m", "mb-l");

        cardsLayout.add(createStatCard("Patients", totalPatients, VaadinIcon.USERS, "#3498db"));
        cardsLayout.add(createStatCard("Examens aujourd'hui", todayExams, VaadinIcon.CALENDAR, "#ff9800"));
        cardsLayout.add(createStatCard("En attente", pendingExams, VaadinIcon.CLOCK, "#f44336"));
        cardsLayout.add(createStatCard("Ce mois", monthCompletedExams, VaadinIcon.CHECK_CIRCLE, "#4caf50"));

        return cardsLayout;
    }

    private Div createStatCard(String title, int value, VaadinIcon icon, String color) {
        Div card = new Div();
        card.addClassNames("stat-card", "p-m", "border-radius-m", "shadow-s");
        card.getStyle().set("background-color", "white");
        card.getStyle().set("border", "1px solid var(--lumo-contrast-20pct)");
        card.setWidthFull();

        VerticalLayout cardContent = new VerticalLayout();
        cardContent.setPadding(false);
        cardContent.setSpacing(false);
        cardContent.setAlignItems(Alignment.CENTER);

        // Icône
        Div iconContainer = new Div();
        iconContainer.add(icon.create());
        iconContainer.getStyle().set("color", color);
        iconContainer.getStyle().set("font-size", "24px");
        iconContainer.addClassNames("mb-s");

        // Valeur
        Span valueSpan = new Span(String.valueOf(value));
        valueSpan.addClassNames("text-2xl", "font-bold", "mb-xs");
        valueSpan.getStyle().set("color", color);

        // Titre
        Span titleSpan = new Span(title);
        titleSpan.addClassNames("text-s", "text-secondary");

        cardContent.add(iconContainer, valueSpan, titleSpan);
        card.add(cardContent);

        return card;
    }

    private Component createQuickActions() {
        HorizontalLayout actionsLayout = new HorizontalLayout();
        actionsLayout.setWidthFull();
        actionsLayout.addClassNames("gap-m", "mb-l");

        // Boutons selon le rôle
        if (SecurityUtils.hasAnyRole(UserRole.ADMIN, UserRole.MEDECIN, UserRole.SECRETAIRE)) {
            Button newPatientBtn = new Button("Nouveau patient", VaadinIcon.PLUS.create());
            newPatientBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            newPatientBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("patients")));
            actionsLayout.add(newPatientBtn);
        }

        if (SecurityUtils.hasAnyRole(UserRole.ADMIN, UserRole.MEDECIN, UserRole.SECRETAIRE)) {
            Button newExamBtn = new Button("Nouvel examen", VaadinIcon.PLUS.create());
            newExamBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            newExamBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("exams")));
            actionsLayout.add(newExamBtn);
        }

        if (SecurityUtils.hasAnyRole(UserRole.ADMIN, UserRole.TECHNICIEN)) {
            Button mwlBtn = new Button("Worklist MWL", VaadinIcon.LIST.create());
            mwlBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            mwlBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("mwl")));
            actionsLayout.add(mwlBtn);
        }

        return actionsLayout;
    }

    private Component createRecentExamsSection() {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.addClassNames("bg-contrast-5pct", "border-radius-m", "p-m");

        H3 sectionTitle = new H3("Examens d'aujourd'hui");
        sectionTitle.addClassNames("mb-m", "text-l");
        section.add(sectionTitle);

        Grid<Exam> grid = new Grid<>();
        grid.setWidthFull();
        grid.setHeight("400px");
        grid.addClassNames("border-radius-m");

        // Colonnes
        grid.addColumn(exam -> exam.getPatient().getFullName())
                .setHeader("Patient")
                .setSortable(true);

        grid.addColumn(exam -> exam.getModalityCode() != null ? exam.getModalityCode() : "N/A")
                .setHeader("Modalité")
                .setSortable(true);

        grid.addColumn(exam -> exam.getScheduledDateTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .setHeader("Heure")
                .setSortable(true);

        grid.addColumn(new ComponentRenderer<>(exam -> createStatusBadge(exam.getStatus())))
                .setHeader("Statut")
                .setSortable(true);

        // Limiter à 10 examens
        List<Exam> limitedExams = recentExams.stream()
                .limit(10)
                .toList();
        
        grid.setItems(limitedExams);

        section.add(grid);
        return section;
    }

    private Span createStatusBadge(ExamStatus status) {
        Span badge = new Span(status.toString());
        badge.addClassNames("badge", "text-s", "font-semibold", "p-xs");
        
        switch (status) {
            case CREATED:
                badge.getStyle().set("background-color", "#f5f5f5");
                badge.getStyle().set("color", "#616161");
                break;
            case SELECTED:
                badge.getStyle().set("background-color", "#f3e5f5");
                badge.getStyle().set("color", "#7b1fa2");
                break;
            case PLANNED:
                badge.getStyle().set("background-color", "#e3f2fd");
                badge.getStyle().set("color", "#1976d2");
                break;
            case IN_PROGRESS:
                badge.getStyle().set("background-color", "#fff3e0");
                badge.getStyle().set("color", "#f57c00");
                break;
            case COMPLETED:
                badge.getStyle().set("background-color", "#e8f5e8");
                badge.getStyle().set("color", "#388e3c");
                break;
            case CANCELLED:
                badge.getStyle().set("background-color", "#ffebee");
                badge.getStyle().set("color", "#d32f2f");
                break;
            default:
                badge.getStyle().set("background-color", "#f5f5f5");
                badge.getStyle().set("color", "#616161");
                break;
        }
        
        return badge;
    }
}
