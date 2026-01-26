package com.application.views;

import com.application.entity.*;
import com.application.entity.User;
import com.application.repository.ExamRepository;
import com.application.repository.ReportRepository;
import com.application.security.SecurityUtils;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Route(value = "reports", layout = MainLayout.class)
@PageTitle("Rapports radiologiques")
@AnonymousAllowed
public class ReportView extends VerticalLayout {

    private final ReportRepository reportRepository;
    private final ExamRepository examRepository;

    // UI Components
    private final Grid<Exam> grid = new Grid<>(Exam.class);
    private final Grid<Report> reportsGrid = new Grid<>(Report.class);
    private final TextField searchField = new TextField();
    private final Span examCountBadge = new Span();

    private User currentUser;
    private List<Exam> allExams = new ArrayList<>();

    public ReportView(ExamRepository examRepository, ReportRepository reportRepository) {
        this.examRepository = examRepository;
        this.reportRepository = reportRepository;

        setWidthFull();
        setPadding(false);
        setSpacing(false);
        addClassName("report-view");

        // 1. En-tête stylisé
        add(createStyledHeader());

        // 2. Barre de recherche et filtres
        add(createSearchBar());

        // 3. Configuration de la Grille
        configureGrid();

        // 4. Configuration de la Grille des rapports
        configureReportsGrid();

        // 5. Mise en page principale
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setSizeFull();
        mainLayout.setPadding(true);
        mainLayout.setSpacing(true);

        grid.setHeight("calc(50vh - 150px)");
        mainLayout.add(grid);

        // 6. Tableau des rapports validés
        mainLayout.add(createReportsSection());

        add(mainLayout);

        // Charger les données
        refreshGrid();
    }

    @PostConstruct
    public void init() {
        currentUser = SecurityUtils.getCurrentUser().orElse(null);
    }

    private Component createStyledHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setPadding(true);
        header.setSpacing(true);
        header.setAlignItems(Alignment.CENTER);
        header.getStyle()
                .set("background", "linear-gradient(135deg, #3b82f6 0%, #8b5cf6 100%)")
                .set("color", "white")
                .set("box-shadow", "0 4px 6px rgba(0,0,0,0.1)")
                .set("border-radius", "0 0 16px 16px");

        // Icône et titre
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(Alignment.CENTER);
        titleLayout.setSpacing(true);

        Icon reportIcon = VaadinIcon.FILE_TEXT_O.create();
        reportIcon.setSize("32px");
        reportIcon.getStyle().set("color", "white");

        H2 title = new H2("Rapports Radiologiques");
        title.getStyle()
                .set("margin", "0")
                .set("color", "white")
                .set("font-weight", "600");

        titleLayout.add(reportIcon, title);

        // Badge compteur
        examCountBadge.getStyle()
                .set("background-color", "rgba(255,255,255,0.2)")
                .set("color", "white")
                .set("padding", "0.5rem 1rem")
                .set("border-radius", "2rem")
                .set("font-weight", "600")
                .set("font-size", "14px");

        // Bouton actualiser
        Button refreshBtn = new Button("Actualiser", VaadinIcon.REFRESH.create());
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        refreshBtn.getStyle()
                .set("background", "rgba(255,255,255,0.2)")
                .set("color", "white");
        refreshBtn.addClickListener(e -> {
            refreshGrid();
            Notification.show("Liste actualisée", 2000, Notification.Position.BOTTOM_END);
        });

        header.add(titleLayout, examCountBadge);
        header.setFlexGrow(1, titleLayout);
        header.add(refreshBtn);

        return header;
    }

    private Component createSearchBar() {
        HorizontalLayout searchBar = new HorizontalLayout();
        searchBar.setWidthFull();
        searchBar.setPadding(true);
        searchBar.setSpacing(true);
        searchBar.setAlignItems(Alignment.CENTER);
        searchBar.getStyle()
                .set("background-color", "#f8fafc")
                .set("border-bottom", "1px solid #e2e8f0");

        searchField.setPlaceholder("Rechercher par patient, accession, modalité...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setWidth("400px");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setValueChangeTimeout(300);
        searchField.addValueChangeListener(e -> filterExams(e.getValue()));

        // Filtres rapides
        Button allBtn = new Button("Tous", VaadinIcon.LIST.create());
        allBtn.addThemeVariants(ButtonVariant.LUMO_SMALL);
        allBtn.addClickListener(e -> filterByStatus(null));

        Button completedBtn = new Button("Terminés", VaadinIcon.CHECK.create());
        completedBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_SUCCESS);
        completedBtn.addClickListener(e -> filterByStatus(ExamStatus.COMPLETED));

        Button inProgressBtn = new Button("En cours", VaadinIcon.CLOCK.create());
        inProgressBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        inProgressBtn.addClickListener(e -> filterByStatus(ExamStatus.IN_PROGRESS));

        HorizontalLayout filters = new HorizontalLayout(allBtn, completedBtn, inProgressBtn);
        filters.setSpacing(true);

        searchBar.add(searchField);
        searchBar.setFlexGrow(1, searchField);
        searchBar.add(filters);

        return searchBar;
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addClassName("report-grid");
        grid.getStyle()
                .set("border-radius", "12px")
                .set("overflow", "hidden");

        // Colonne Accession avec icône - CLIQUABLE
        grid.addColumn(new ComponentRenderer<>(exam -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setAlignItems(Alignment.CENTER);
            layout.setSpacing(true);
            layout.getStyle()
                    .set("cursor", "pointer")
                    .set("user-select", "none");

            Icon icon = VaadinIcon.CLIPBOARD_TEXT.create();
            icon.setSize("16px");
            icon.getStyle().set("color", "#3b82f6");

            Span accession = new Span(exam.getAccessionNumber());
            accession.getStyle()
                    .set("font-weight", "600")
                    .set("color", "#3b82f6")
                    .set("text-decoration", "underline");

            layout.add(icon, accession);
            layout.addClickListener(e -> openSplitViewDialog(exam));

            return layout;
        })).setHeader("N° Accession").setSortable(true).setWidth("180px");

        // Colonne Patient avec avatar
        grid.addColumn(new ComponentRenderer<>(exam -> {
            if (exam.getPatient() == null) return new Span("N/A");

            HorizontalLayout layout = new HorizontalLayout();
            layout.setAlignItems(Alignment.CENTER);
            layout.setSpacing(true);

            Avatar avatar = new Avatar();
            avatar.setName(exam.getPatient().getFirstName() + " " + exam.getPatient().getLastName());
            avatar.getStyle()
                    .set("width", "32px")
                    .set("height", "32px");

            VerticalLayout info = new VerticalLayout();
            info.setSpacing(false);
            info.setPadding(false);

            Span name = new Span(exam.getPatient().getFirstName() + " " + exam.getPatient().getLastName());
            name.getStyle()
                    .set("font-weight", "600")
                    .set("color", "#1e293b");

            Span ipp = new Span("IPP: " + exam.getPatient().getPatientId());
            ipp.getStyle()
                    .set("font-size", "12px")
                    .set("color", "#64748b");

            info.add(name, ipp);
            layout.add(avatar, info);

            return layout;
        })).setHeader("Patient").setFlexGrow(1);

        // Colonne Modalité avec badge
        grid.addColumn(new ComponentRenderer<>(exam -> {
            Span badge = new Span(exam.getModality());
            badge.getStyle()
                    .set("background-color", getModalityColor(exam.getModality()))
                    .set("color", "white")
                    .set("padding", "0.25rem 0.75rem")
                    .set("border-radius", "1rem")
                    .set("font-size", "12px")
                    .set("font-weight", "600");
            return badge;
        })).setHeader("Modalité").setWidth("120px");

        // Colonne Type
        grid.addColumn(exam -> exam.getExamType() != null ? exam.getExamType().toString() : "N/A")
                .setHeader("Type")
                .setWidth("150px");

        // Colonne Date avec icône
        grid.addColumn(new ComponentRenderer<>(exam -> {
            if (exam.getScheduledDateTime() == null) return new Span("N/A");

            HorizontalLayout layout = new HorizontalLayout();
            layout.setAlignItems(Alignment.CENTER);
            layout.setSpacing(true);

            Icon icon = VaadinIcon.CALENDAR.create();
            icon.setSize("14px");
            icon.getStyle().set("color", "#64748b");

            Span date = new Span(exam.getScheduledDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            date.getStyle().set("font-size", "13px");

            layout.add(icon, date);
            return layout;
        })).setHeader("Date programmée").setWidth("180px");

        // Colonne Médecin
        grid.addColumn(exam -> exam.getMedecin() != null ?
                        "Dr. " + exam.getMedecin().getLastName() : "N/A")
                .setHeader("Médecin")
                .setWidth("150px");

        // Colonne Statut avec badges colorés
        grid.addColumn(new ComponentRenderer<>(exam -> {
            String statut = exam.getStatus() != null ? exam.getStatus().toString() : "INCONNU";
            Span badge = new Span(getStatusLabel(statut));

            badge.getStyle()
                    .set("padding", "0.25rem 0.75rem")
                    .set("border-radius", "1rem")
                    .set("font-size", "12px")
                    .set("font-weight", "600");

            if ("COMPLETED".equalsIgnoreCase(statut)) {
                badge.getStyle()
                        .set("background-color", "#10b981")
                        .set("color", "white");
            } else if ("IN_PROGRESS".equalsIgnoreCase(statut)) {
                badge.getStyle()
                        .set("background-color", "#3b82f6")
                        .set("color", "white");
            } else {
                badge.getStyle()
                        .set("background-color", "#94a3b8")
                        .set("color", "white");
            }

            return badge;
        })).setHeader("État").setWidth("130px");

        // Colonne Rapport avec indicateur
        grid.addColumn(new ComponentRenderer<>(exam -> {
            if (exam.getReport() != null) {
                HorizontalLayout layout = new HorizontalLayout();
                layout.setAlignItems(Alignment.CENTER);
                layout.setSpacing(true);

                Icon icon = exam.getReport().getValidated() ?
                        VaadinIcon.CHECK_CIRCLE.create() : VaadinIcon.EDIT.create();
                icon.setSize("16px");
                icon.getStyle().set("color", exam.getReport().getValidated() ? "#10b981" : "#f59e0b");

                Span text = new Span(exam.getReport().getValidated() ? "Validé" : "Brouillon");
                text.getStyle()
                        .set("color", exam.getReport().getValidated() ? "#10b981" : "#f59e0b")
                        .set("font-weight", "600");

                layout.add(icon, text);
                return layout;
            }
            return new Span("-");
        })).setHeader("Rapport").setWidth("130px");
    }

    private void openSplitViewDialog(Exam exam) {
        if (exam == null) return;

        String statut = exam.getStatus() != null ? exam.getStatus().toString() : "UNKNOWN";
        boolean isReady = "COMPLETED".equalsIgnoreCase(statut) || "TERMINE".equalsIgnoreCase(statut);

        if (!isReady) {
            Notification notification = Notification.show(
                    "⚠️ Impossible d'afficher les images : L'examen n'est pas encore terminé (Statut: " + statut + ")",
                    4000,
                    Notification.Position.MIDDLE
            );
            notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        Dialog splitDialog = new Dialog();
        splitDialog.setWidth("95vw");
        splitDialog.setHeight("90vh");
        splitDialog.setResizable(false);
        splitDialog.setDraggable(false);

        String patientName = exam.getPatient() != null ?
                exam.getPatient().getFirstName() + " " + exam.getPatient().getLastName() : "Patient";
        splitDialog.setHeaderTitle("🔬 " + patientName + " - " + exam.getAccessionNumber());

        // Créer le SplitLayout
        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(55); // 55% pour OHIF, 45% pour l'éditeur

        // Partie gauche - OHIF Viewer
        VerticalLayout ohifLayout = new VerticalLayout();
        ohifLayout.setSizeFull();
        ohifLayout.setPadding(false);
        ohifLayout.setSpacing(false);
        ohifLayout.getStyle()
                .set("background-color", "#000000");

        String studyUid = exam.getStudyInstanceUID() != null ?
                exam.getStudyInstanceUID() :
                "1.2.840.10008.5.1.4.1.1.1." + exam.getId();

        String url = "http://localhost:8042/ohif/viewer?StudyInstanceUIDs=" + studyUid;

        IFrame iframe = new IFrame(url);
        iframe.setSizeFull();
        iframe.getStyle()
                .set("border", "none")
                .set("display", "block");

        ohifLayout.add(iframe);

        // Partie droite - Éditeur de rapport
        VerticalLayout editorLayout = new VerticalLayout();
        editorLayout.setSizeFull();
        editorLayout.setPadding(true);
        editorLayout.setSpacing(true);
        editorLayout.getStyle()
                .set("background-color", "#f8fafc");

        // En-tête de l'éditeur
        HorizontalLayout editorHeader = new HorizontalLayout();
        editorHeader.setWidthFull();
        editorHeader.setAlignItems(Alignment.CENTER);
        editorHeader.getStyle()
                .set("padding-bottom", "1rem")
                .set("border-bottom", "2px solid #e2e8f0");

        Icon editIcon = VaadinIcon.EDIT.create();
        editIcon.setSize("24px");
        editIcon.getStyle().set("color", "#3b82f6");

        H3 editorTitle = new H3("Rédaction du Rapport");
        editorTitle.getStyle()
                .set("margin", "0")
                .set("color", "#1e293b");

        editorHeader.add(editIcon, editorTitle);

        // Informations de l'examen
        VerticalLayout examInfo = new VerticalLayout();
        examInfo.setSpacing(false);
        examInfo.setPadding(false);
        examInfo.getStyle()
                .set("background-color", "#e0f2fe")
                .set("padding", "0.75rem")
                .set("border-radius", "8px")
                .set("margin-bottom", "1rem");

        Span modalityInfo = new Span("📊 " + exam.getModality() + " - " +
                (exam.getExamType() != null ? exam.getExamType().toString() : "N/A"));
        modalityInfo.getStyle()
                .set("font-weight", "600")
                .set("color", "#0369a1");

        Span dateInfo = new Span("📅 " +
                (exam.getScheduledDateTime() != null ?
                        exam.getScheduledDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A"));
        dateInfo.getStyle()
                .set("font-size", "13px")
                .set("color", "#0c4a6e");

        examInfo.add(modalityInfo, dateInfo);

        // Champs de saisie
        TextArea findingsField = new TextArea("Observations détaillées");
        findingsField.setWidthFull();
        findingsField.setHeight("250px");
        findingsField.setPlaceholder("Décrivez vos observations ici...");
        findingsField.getStyle()
                .set("border-radius", "8px")
                .set("font-family", "monospace")
                .set("font-size", "14px");

        TextArea conclusionField = new TextArea("Conclusion");
        conclusionField.setWidthFull();
        conclusionField.setHeight("120px");
        conclusionField.setPlaceholder("Conclusion clinique...");
        conclusionField.getStyle()
                .set("border-radius", "8px")
                .set("font-family", "monospace")
                .set("font-size", "14px");

        // Charger les données existantes
        if (exam.getReport() != null) {
            findingsField.setValue(exam.getReport().getFindings() != null ? exam.getReport().getFindings() : "");
            conclusionField.setValue(exam.getReport().getConclusion() != null ? exam.getReport().getConclusion() : "");
        }

        // Boutons d'action
        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setWidthFull();
        buttons.setSpacing(true);
        buttons.setJustifyContentMode(JustifyContentMode.END);

        Button saveButton = new Button(exam.getReport() != null ? "Modifier le Rapport" : "Valider le Rapport");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        saveButton.setIcon(VaadinIcon.CHECK.create());
        saveButton.getStyle()
                .set("background", "linear-gradient(135deg, #10b981 0%, #059669 100%)")
                .set("border", "none");

        saveButton.setEnabled(canModifyReport(exam));

        saveButton.addClickListener(e -> {
            Report report = exam.getReport();
            if (report == null) {
                report = new Report();
                report.setExam(exam);
                report.setRadiologue(currentUser);
                exam.setReport(report);
            }

            report.setFindings(findingsField.getValue());
            report.setConclusion(conclusionField.getValue());
            report.setValidated(true);
            report.setValidatedAt(LocalDateTime.now());

            exam.setStatus(ExamStatus.COMPLETED);

            reportRepository.save(report);
            examRepository.save(exam);

            Notification notification = Notification.show(
                    "✅ Rapport validé et sauvegardé !",
                    3000,
                    Notification.Position.BOTTOM_END
            );
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            refreshGrid();
            refreshReportsGrid();
            splitDialog.close();
        });

        Button cancelButton = new Button("Fermer", e -> splitDialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttons.add(cancelButton, saveButton);

        editorLayout.add(editorHeader, examInfo, findingsField, conclusionField, buttons);

        // Ajouter les deux parties au SplitLayout
        splitLayout.addToPrimary(ohifLayout);
        splitLayout.addToSecondary(editorLayout);

        splitDialog.add(splitLayout);
        splitDialog.open();
    }

    private String getStatusLabel(String status) {
        return switch (status.toUpperCase()) {
            case "COMPLETED" -> "Terminé";
            case "IN_PROGRESS" -> "En cours";
            case "PLANNED" -> "Planifié";
            case "SELECTED" -> "Sélectionné";
            default -> status;
        };
    }

    private String getModalityColor(String modality) {
        return switch (modality) {
            case "CT" -> "#3b82f6";
            case "MR", "IRM" -> "#8b5cf6";
            case "US" -> "#06b6d4";
            case "XR", "CR", "DX" -> "#10b981";
            default -> "#64748b";
        };
    }

    private boolean canModifyReport(Exam exam) {
        if (currentUser == null) return false;

        UserRole userRole = currentUser.getRole();

        switch (userRole) {
            case ADMIN:
                return true;
            case RADIOLOGUE:
                return true;
            case MEDECIN:
                return exam.getMedecin() != null &&
                        exam.getMedecin().getId().equals(currentUser.getId());
            default:
                return false;
        }
    }

    private void filterExams(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            grid.setItems(allExams);
        } else {
            String term = searchTerm.toLowerCase().trim();
            List<Exam> filtered = allExams.stream()
                    .filter(exam ->
                            exam.getAccessionNumber().toLowerCase().contains(term) ||
                                    exam.getModality().toLowerCase().contains(term) ||
                                    (exam.getPatient() != null &&
                                            (exam.getPatient().getFirstName().toLowerCase().contains(term) ||
                                                    exam.getPatient().getLastName().toLowerCase().contains(term) ||
                                                    exam.getPatient().getPatientId().toLowerCase().contains(term)))
                    )
                    .toList();
            grid.setItems(filtered);
        }
        updateExamCount();
    }

    private void filterByStatus(ExamStatus status) {
        if (status == null) {
            grid.setItems(allExams);
        } else {
            List<Exam> filtered = allExams.stream()
                    .filter(exam -> exam.getStatus() == status)
                    .toList();
            grid.setItems(filtered);
        }
        updateExamCount();
    }

    private void updateExamCount() {
        int count = grid.getListDataView().getItemCount();
        examCountBadge.setText(count + " examen" + (count > 1 ? "s" : ""));
    }

    private void refreshGrid() {
        List<Exam> exams;
        if (currentUser != null && currentUser.getRole() == UserRole.RADIOLOGUE) {
            exams = examRepository.findAllWithRelations();
        } else if (currentUser != null && currentUser.getRole() == UserRole.MEDECIN) {
            exams = examRepository.findByMedecinWithRelations(currentUser.getId());
        } else {
            exams = examRepository.findAllWithRelations();
        }

        allExams = exams;
        grid.setItems(exams);
        updateExamCount();
    }

    private void configureReportsGrid() {
        reportsGrid.setSizeFull();
        reportsGrid.setHeight("400px");
        reportsGrid.removeAllColumns();
        reportsGrid.addClassName("reports-grid");
        reportsGrid.getStyle()
                .set("border-radius", "12px")
                .set("overflow", "hidden");

        // Colonne Patient
        reportsGrid.addColumn(report -> {
            if (report.getExam() != null && report.getExam().getPatient() != null) {
                return report.getExam().getPatient().getFirstName() + " " +
                        report.getExam().getPatient().getLastName();
            }
            return "N/A";
        }).setHeader("Patient").setSortable(true).setFlexGrow(1);

        // Colonne Accession
        reportsGrid.addColumn(report -> {
            if (report.getExam() != null) {
                return report.getExam().getAccessionNumber();
            }
            return "N/A";
        }).setHeader("N° Accession").setSortable(true).setWidth("150px");

        // Colonne Modalité
        reportsGrid.addColumn(report -> {
            if (report.getExam() != null) {
                return report.getExam().getModality();
            }
            return "N/A";
        }).setHeader("Modalité").setWidth("100px");

        // Colonne Radiologue
        reportsGrid.addColumn(report -> {
            if (report.getRadiologue() != null) {
                return "Dr. " + report.getRadiologue().getLastName();
            }
            return "N/A";
        }).setHeader("Radiologue").setWidth("150px");

        // Colonne Date de validation
        reportsGrid.addColumn(report -> {
            if (report.getValidatedAt() != null) {
                return report.getValidatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            }
            return "N/A";
        }).setHeader("Date validation").setWidth("150px");

        // Colonne Statut
        reportsGrid.addColumn(new ComponentRenderer<>(report -> {
            Span badge = new Span(report.getValidated() ? "Validé" : "Brouillon");
            badge.getStyle()
                    .set("padding", "0.25rem 0.75rem")
                    .set("border-radius", "1rem")
                    .set("font-size", "12px")
                    .set("font-weight", "600");

            if (report.getValidated()) {
                badge.getStyle()
                        .set("background-color", "#10b981")
                        .set("color", "white");
            } else {
                badge.getStyle()
                        .set("background-color", "#f59e0b")
                        .set("color", "white");
            }

            return badge;
        })).setHeader("Statut").setWidth("100px");

        // Colonne Actions
        reportsGrid.addColumn(new ComponentRenderer<>(report -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(false);

            Button viewBtn = new Button(VaadinIcon.EYE.create());
            viewBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            viewBtn.getElement().setProperty("title", "Voir le rapport");
            viewBtn.addClickListener(e -> openReportDialog(report));

            Button editBtn = new Button(VaadinIcon.EDIT.create());
            editBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
            editBtn.getElement().setProperty("title", "Modifier le rapport");
            editBtn.addClickListener(e -> openSplitViewDialog(report.getExam()));
            editBtn.setEnabled(canEditReport(report));

            Button deleteBtn = new Button(VaadinIcon.TRASH.create());
            deleteBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            deleteBtn.getElement().setProperty("title", "Supprimer le rapport");
            deleteBtn.addClickListener(e -> deleteReport(report));
            deleteBtn.setEnabled(canDeleteReport(report));

            actions.add(viewBtn, editBtn, deleteBtn);
            return actions;
        })).setHeader("Actions").setWidth("180px");
    }

    private boolean canEditReport(Report report) {
        if (currentUser == null) return false;

        UserRole userRole = currentUser.getRole();

        switch (userRole) {
            case ADMIN:
                return true;
            case RADIOLOGUE:
                return report.getRadiologue() != null &&
                        report.getRadiologue().getId().equals(currentUser.getId());
            case MEDECIN:
                return report.getExam() != null && report.getExam().getMedecin() != null &&
                        report.getExam().getMedecin().getId().equals(currentUser.getId());
            default:
                return false;
        }
    }

    private boolean canDeleteReport(Report report) {
        if (currentUser == null) return false;

        UserRole userRole = currentUser.getRole();

        switch (userRole) {
            case ADMIN:
                return true;
            case RADIOLOGUE:
                return report.getRadiologue() != null &&
                        report.getRadiologue().getId().equals(currentUser.getId());
            default:
                return false;
        }
    }

    private void deleteReport(Report report) {
        com.vaadin.flow.component.confirmdialog.ConfirmDialog dialog = new com.vaadin.flow.component.confirmdialog.ConfirmDialog();
        dialog.setHeader("⚠️ Confirmation de suppression");
        dialog.setText("Êtes-vous sûr de vouloir supprimer ce rapport ? Cette action est irréversible.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Supprimer");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(e -> {
            try {
                reportRepository.delete(report);

                if (report.getExam() != null) {
                    Exam exam = report.getExam();
                    exam.setReport(null);
                    exam.setStatus(ExamStatus.COMPLETED);
                    examRepository.save(exam);
                }

                refreshGrid();
                refreshReportsGrid();

                Notification notification = Notification.show(
                        "🗑️ Rapport supprimé avec succès",
                        3000,
                        Notification.Position.BOTTOM_END
                );
                notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            } catch (Exception ex) {
                Notification notification = Notification.show(
                        "❌ Erreur lors de la suppression: " + ex.getMessage(),
                        4000,
                        Notification.Position.BOTTOM_END
                );
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        dialog.open();
    }

    private Component createReportsSection() {
        VerticalLayout section = new VerticalLayout();
        section.setWidthFull();
        section.setPadding(true);
        section.setSpacing(true);
        section.getStyle()
                .set("background-color", "#f8fafc")
                .set("border-top", "2px solid #e2e8f0")
                .set("margin-top", "1rem");

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);

        H3 title = new H3("Rapports validés");
        title.getStyle()
                .set("margin", "0")
                .set("color", "#1e293b")
                .set("font-weight", "600");

        Button refreshReportsBtn = new Button("Actualiser", VaadinIcon.REFRESH.create());
        refreshReportsBtn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        refreshReportsBtn.addClickListener(e -> refreshReportsGrid());

        header.add(title, refreshReportsBtn);

        refreshReportsGrid();

        section.add(header, reportsGrid);
        section.setFlexGrow(1, reportsGrid);

        return section;
    }

    private void refreshReportsGrid() {
        try {
            List<Report> reports = reportRepository.findAllWithRelations();
            reportsGrid.setItems(reports);
        } catch (Exception e) {
            Notification.show("Erreur lors du chargement des rapports: " + e.getMessage());
        }
    }

    private void openReportDialog(Report report) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Détails du rapport");
        dialog.setWidth("800px");
        dialog.setHeight("600px");

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);
        content.setSizeFull();

        if (report.getExam() != null) {
            HorizontalLayout examInfo = new HorizontalLayout();
            examInfo.setWidthFull();
            examInfo.setSpacing(true);

            Span accession = new Span("N° Accession: " + report.getExam().getAccessionNumber());
            Span patient = new Span("Patient: " +
                    (report.getExam().getPatient() != null ?
                            report.getExam().getPatient().getFirstName() + " " + report.getExam().getPatient().getLastName() : "N/A"));
            Span modality = new Span("Modalité: " + report.getExam().getModality());

            examInfo.add(accession, patient, modality);
            content.add(examInfo);
        }

        TextArea findingsDisplay = new TextArea("Observations");
        findingsDisplay.setValue(report.getFindings() != null ? report.getFindings() : "");
        findingsDisplay.setReadOnly(true);
        findingsDisplay.setWidthFull();
        findingsDisplay.setHeight("200px");

        TextArea conclusionDisplay = new TextArea("Conclusion");
        conclusionDisplay.setValue(report.getConclusion() != null ? report.getConclusion() : "");
        conclusionDisplay.setReadOnly(true);
        conclusionDisplay.setWidthFull();
        conclusionDisplay.setHeight("100px");

        content.add(findingsDisplay, conclusionDisplay);

        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setWidthFull();
        buttons.setSpacing(true);
        buttons.setJustifyContentMode(JustifyContentMode.END);

        Button viewSplitBtn = new Button("Voir avec OHIF", VaadinIcon.FILM.create());
        viewSplitBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        viewSplitBtn.addClickListener(e -> {
            dialog.close();
            openSplitViewDialog(report.getExam());
        });

        Button closeBtn = new Button("Fermer", e -> dialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        buttons.add(viewSplitBtn, closeBtn);
        content.add(buttons);

        dialog.add(content);
        dialog.open();
    }
}