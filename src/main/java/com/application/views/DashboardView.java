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
        setPadding(false);
        setSpacing(false);

        // Container principal avec padding
        VerticalLayout mainContainer = new VerticalLayout();
        mainContainer.setSizeFull();
        mainContainer.setPadding(true);
        mainContainer.setSpacing(true);
        mainContainer.getStyle()
                .set("padding", "var(--lumo-space-l)")
                .set("max-width", "1400px")
                .set("margin", "0 auto");

        // Titre avec meilleur espacement
        H2 title = new H2();
        title.add(VaadinIcon.DASHBOARD.create());
        title.add(" Tableau de bord");
        title.getStyle()
                .set("margin", "0 0 var(--lumo-space-xl) 0")
                .set("font-size", "var(--lumo-font-size-xxl)")
                .set("font-weight", "600")
                .set("color", "var(--lumo-header-text-color)");

        mainContainer.add(title);

        // Cartes statistiques avec espacement
        mainContainer.add(createStatisticsCards());

        // Boutons d'accès rapide avec espacement
        mainContainer.add(createQuickActions());

        // Section examens récents avec espacement
        mainContainer.add(createRecentExamsSection());

        add(mainContainer);
    }

    private Component createStatisticsCards() {
        HorizontalLayout cardsLayout = new HorizontalLayout();
        cardsLayout.setWidthFull();
        cardsLayout.getStyle()
                .set("gap", "var(--lumo-space-m)")
                .set("margin-bottom", "var(--lumo-space-xl)");

        cardsLayout.add(createStatCard("Patients", totalPatients, VaadinIcon.USERS, "#3498db"));
        cardsLayout.add(createStatCard("Examens aujourd'hui", todayExams, VaadinIcon.CALENDAR, "#ff9800"));
        cardsLayout.add(createStatCard("En attente", pendingExams, VaadinIcon.CLOCK, "#f44336"));
        cardsLayout.add(createStatCard("Ce mois", monthCompletedExams, VaadinIcon.CHECK_CIRCLE, "#4caf50"));

        return cardsLayout;
    }

    private Div createStatCard(String title, int value, VaadinIcon icon, String color) {
        Div card = new Div();
        card.setWidthFull();
        card.getStyle()
                .set("background-color", "white")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "var(--lumo-space-l)")
                .set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.05)")
                .set("transition", "all 0.3s ease")
                .set("cursor", "default");

        // Effet hover
        card.getElement().addEventListener("mouseenter", e -> {
            card.getStyle()
                    .set("box-shadow", "0 4px 12px rgba(0, 0, 0, 0.1)")
                    .set("transform", "translateY(-2px)");
        });

        card.getElement().addEventListener("mouseleave", e -> {
            card.getStyle()
                    .set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.05)")
                    .set("transform", "translateY(0)");
        });

        VerticalLayout cardContent = new VerticalLayout();
        cardContent.setPadding(false);
        cardContent.setSpacing(false);
        cardContent.setAlignItems(Alignment.START);
        cardContent.getStyle().set("gap", "var(--lumo-space-s)");

        // Icône avec background coloré
        Div iconContainer = new Div();
        iconContainer.add(icon.create());
        iconContainer.getStyle()
                .set("color", color)
                .set("font-size", "28px")
                .set("background-color", hexToRgba(color, 0.1))
                .set("width", "56px")
                .set("height", "56px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("margin-bottom", "var(--lumo-space-m)");

        // Valeur
        Span valueSpan = new Span(String.valueOf(value));
        valueSpan.getStyle()
                .set("font-size", "32px")
                .set("font-weight", "700")
                .set("color", "var(--lumo-header-text-color)")
                .set("line-height", "1")
                .set("margin", "0");

        // Titre
        Span titleSpan = new Span(title);
        titleSpan.getStyle()
                .set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-weight", "500")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.5px");

        cardContent.add(iconContainer, valueSpan, titleSpan);
        card.add(cardContent);

        return card;
    }

    private Component createQuickActions() {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setPadding(false);
        section.setSpacing(false);
        section.getStyle().set("margin-bottom", "var(--lumo-space-xl)");

        // Titre de section
        H3 sectionTitle = new H3("Actions rapides");
        sectionTitle.getStyle()
                .set("margin", "0 0 var(--lumo-space-m) 0")
                .set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "600")
                .set("color", "var(--lumo-header-text-color)");

        section.add(sectionTitle);

        HorizontalLayout actionsLayout = new HorizontalLayout();
        actionsLayout.setWidthFull();
        actionsLayout.getStyle()
                .set("gap", "var(--lumo-space-m)")
                .set("flex-wrap", "wrap");

        // Boutons selon le rôle
        if (SecurityUtils.hasAnyRole(UserRole.ADMIN, UserRole.MEDECIN, UserRole.SECRETAIRE)) {
            Button newPatientBtn = new Button("Nouveau patient", VaadinIcon.PLUS.create());
            newPatientBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
            newPatientBtn.getStyle()
                    .set("padding", "var(--lumo-space-m) var(--lumo-space-l)")
                    .set("font-weight", "600");
            newPatientBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("patients")));
            actionsLayout.add(newPatientBtn);
        }

        if (SecurityUtils.hasAnyRole(UserRole.ADMIN, UserRole.MEDECIN, UserRole.SECRETAIRE)) {
            Button newExamBtn = new Button("Nouvel examen", VaadinIcon.PLUS.create());
            newExamBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
            newExamBtn.getStyle()
                    .set("padding", "var(--lumo-space-m) var(--lumo-space-l)")
                    .set("font-weight", "600");
            newExamBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("exams")));
            actionsLayout.add(newExamBtn);
        }

        if (SecurityUtils.hasAnyRole(UserRole.ADMIN, UserRole.TECHNICIEN)) {
            Button mwlBtn = new Button("Worklist MWL", VaadinIcon.LIST.create());
            mwlBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_LARGE);
            mwlBtn.getStyle()
                    .set("padding", "var(--lumo-space-m) var(--lumo-space-l)")
                    .set("font-weight", "600");
            mwlBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("mwl")));
            actionsLayout.add(mwlBtn);
        }

        section.add(actionsLayout);
        return section;
    }

    private Component createRecentExamsSection() {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setPadding(false);
        section.setSpacing(false);

        // Container avec background
        Div container = new Div();
        container.setWidthFull();
        container.getStyle()
                .set("background-color", "white")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("padding", "var(--lumo-space-l)")
                .set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.05)");

        VerticalLayout content = new VerticalLayout();
        content.setWidthFull();
        content.setPadding(false);
        content.setSpacing(false);
        content.getStyle().set("gap", "var(--lumo-space-m)");

        H3 sectionTitle = new H3("Examens d'aujourd'hui");
        sectionTitle.getStyle()
                .set("margin", "0")
                .set("font-size", "var(--lumo-font-size-l)")
                .set("font-weight", "600")
                .set("color", "var(--lumo-header-text-color)");

        Grid<Exam> grid = new Grid<>();
        grid.setWidthFull();
        grid.setHeight("400px");
        grid.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "var(--lumo-border-radius-m)");

        // Colonnes avec meilleur espacement
        grid.addColumn(exam -> exam.getPatient().getFullName())
                .setHeader("Patient")
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setSortable(true);

        grid.addColumn(exam -> exam.getModalityCode() != null ? exam.getModalityCode() : "N/A")
                .setHeader("Modalité")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(exam -> exam.getScheduledDateTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .setHeader("Heure")
                .setAutoWidth(true)
                .setSortable(true);

        grid.addColumn(new ComponentRenderer<>(exam -> createStatusBadge(exam.getStatus())))
                .setHeader("Statut")
                .setAutoWidth(true)
                .setSortable(true);

        // Limiter à 10 examens
        List<Exam> limitedExams = recentExams.stream()
                .limit(10)
                .toList();

        grid.setItems(limitedExams);

        // Message si aucun examen
        if (limitedExams.isEmpty()) {
            Div emptyState = new Div();
            emptyState.getStyle()
                    .set("padding", "var(--lumo-space-xl)")
                    .set("text-align", "center")
                    .set("color", "var(--lumo-secondary-text-color)");

            Span emptyIcon = new Span(VaadinIcon.CALENDAR.create());
            emptyIcon.getStyle()
                    .set("font-size", "48px")
                    .set("display", "block")
                    .set("margin-bottom", "var(--lumo-space-m)")
                    .set("opacity", "0.3");

            Span emptyText = new Span("Aucun examen prévu aujourd'hui");
            emptyText.getStyle()
                    .set("font-size", "var(--lumo-font-size-l)")
                    .set("display", "block");

            emptyState.add(emptyIcon, emptyText);
            content.add(sectionTitle, emptyState);
        } else {
            content.add(sectionTitle, grid);
        }

        container.add(content);
        section.add(container);
        return section;
    }

    private Span createStatusBadge(ExamStatus status) {
        Span badge = new Span(getStatusLabel(status));
        badge.getStyle()
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-s)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("font-size", "var(--lumo-font-size-xs)")
                .set("font-weight", "600")
                .set("text-transform", "uppercase")
                .set("letter-spacing", "0.5px")
                .set("white-space", "nowrap")
                .set("display", "inline-block");

        switch (status) {
            case CREATED:
                badge.getStyle()
                        .set("background-color", "#f5f5f5")
                        .set("color", "#616161");
                break;
            case SELECTED:
                badge.getStyle()
                        .set("background-color", "#f3e5f5")
                        .set("color", "#7b1fa2");
                break;
            case PLANNED:
                badge.getStyle()
                        .set("background-color", "#e3f2fd")
                        .set("color", "#1976d2");
                break;
            case IN_PROGRESS:
                badge.getStyle()
                        .set("background-color", "#fff3e0")
                        .set("color", "#f57c00");
                break;
            case COMPLETED:
                badge.getStyle()
                        .set("background-color", "#e8f5e9")
                        .set("color", "#388e3c");
                break;
            case CANCELLED:
                badge.getStyle()
                        .set("background-color", "#ffebee")
                        .set("color", "#d32f2f");
                break;
            default:
                badge.getStyle()
                        .set("background-color", "#f5f5f5")
                        .set("color", "#616161");
                break;
        }

        return badge;
    }

    private String getStatusLabel(ExamStatus status) {
        switch (status) {
            case CREATED: return "Créé";
            case SELECTED: return "Sélectionné";
            case PLANNED: return "Planifié";
            case IN_PROGRESS: return "En cours";
            case COMPLETED: return "Terminé";
            case CANCELLED: return "Annulé";
            default: return status.toString();
        }
    }

    private String hexToRgba(String hex, double alpha) {
        // Conversion simple hex to rgba
        int r = Integer.valueOf(hex.substring(1, 3), 16);
        int g = Integer.valueOf(hex.substring(3, 5), 16);
        int b = Integer.valueOf(hex.substring(5, 7), 16);
        return String.format("rgba(%d, %d, %d, %.2f)", r, g, b, alpha);
    }
}