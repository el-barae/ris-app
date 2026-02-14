package com.application.views;

import com.application.config.ApplicationProperties;
import com.application.entity.*;
import com.application.repository.ExamRepository;
import com.application.repository.ReportRepository;
import com.application.security.SecurityUtils;

// --- IMPORTS PDF (OpenPDF) ---
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

// --- IMPORTS VAADIN ---
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.security.RolesAllowed;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Route(value = "reports", layout = MainLayout.class)
@PageTitle("Rapports radiologiques")
@RolesAllowed({"ADMIN", "RADIOLOGUE"})
public class ReportView extends VerticalLayout {

    // =================================================================
    // CONFIGURATION
    // =================================================================
    private final String ohifBaseUrl;

    private final ReportRepository reportRepository;
    private final ExamRepository examRepository;

    // UI Components
    private final Grid<Exam> grid = new Grid<>(Exam.class);
    private final Grid<Report> reportsGrid = new Grid<>(Report.class);
    private final TextField searchField = new TextField();

    // Conteneurs pour les onglets
    private VerticalLayout worklistContainer;
    private VerticalLayout historyContainer;

    private User currentUser;
    private List<Exam> allExams = new ArrayList<>();

    public ReportView(ExamRepository examRepository, ReportRepository reportRepository, ApplicationProperties applicationProperties) {
        this.examRepository = examRepository;
        this.reportRepository = reportRepository;
        this.ohifBaseUrl = applicationProperties.getOhifBaseUrl();

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("background-color", "#f5f5f5");
        addClassName("report-view");

        add(createStyledHeader());
        add(createMainContent());

        refreshGrid();
    }

    @PostConstruct
    public void init() {
        currentUser = SecurityUtils.getCurrentUser().orElse(null);
    }

    // =================================================================
    // SECTION 1: HEADER & LAYOUT
    // =================================================================

    private Component createStyledHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setPadding(true);
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

        Icon logoIcon = VaadinIcon.DOCTOR.create();
        logoIcon.setSize("28px");
        logoIcon.setColor("white");

        // --- MODIFICATION 1 : CHANGEMENT DU TITRE ---
        H3 title = new H3("Liste des Examens et Rapports");
        title.getStyle().set("color", "white").set("margin", "0");

        HorizontalLayout titleLayout = new HorizontalLayout(logoIcon, title);
        titleLayout.setAlignItems(Alignment.CENTER);

        Span spacer = new Span();
        header.setFlexGrow(1, spacer);

        if (currentUser != null) {
            Avatar userAvatar = new Avatar(currentUser.getFirstName() + " " + currentUser.getLastName());
            userAvatar.getStyle().set("background-color", "rgba(255,255,255,0.2)");
            userAvatar.setTooltipEnabled(true);
            header.add(titleLayout, spacer, userAvatar);
        } else {
            header.add(titleLayout, spacer);
        }
        return header;
    }

    private Component createMainContent() {
        VerticalLayout contentLayout = new VerticalLayout();
        contentLayout.setSizeFull();
        contentLayout.setPadding(true);
        contentLayout.setSpacing(true);

        // --- MODIFICATION 2 : LARGEUR MAXIMALE PASSÉE À 100% (PLEIN ÉCRAN) ---
        contentLayout.setMaxWidth("100%");
        contentLayout.getStyle().set("margin", "0 auto");

        Tab tabWorklist = new Tab(VaadinIcon.LIST.create(), new Span("À traiter / En cours"));
        Tab tabHistory = new Tab(VaadinIcon.ARCHIVE.create(), new Span("Historique & Validés"));

        Tabs tabs = new Tabs(tabWorklist, tabHistory);
        tabs.addThemeVariants(TabsVariant.LUMO_EQUAL_WIDTH_TABS);
        tabs.setWidthFull();
        tabs.getStyle().set("background-color", "transparent");

        worklistContainer = createWorklistContainer();
        historyContainer = createHistoryContainer();
        historyContainer.setVisible(false);

        tabs.addSelectedChangeListener(event -> {
            if (event.getSelectedTab().equals(tabWorklist)) {
                worklistContainer.setVisible(true);
                historyContainer.setVisible(false);
                refreshGrid();
            } else {
                worklistContainer.setVisible(false);
                historyContainer.setVisible(true);
                refreshReportsGrid();
            }
        });

        contentLayout.add(tabs, worklistContainer, historyContainer);
        return contentLayout;
    }

    // =================================================================
    // SECTION 2: WORKLIST (GRILLE PRINCIPALE)
    // =================================================================

    private VerticalLayout createWorklistContainer() {
        VerticalLayout container = new VerticalLayout();
        container.setSizeFull();
        container.setPadding(true);
        container.setSpacing(true);
        container.getStyle()
                .set("background-color", "white")
                .set("border-radius", "12px")
                .set("box-shadow", "0 1px 3px 0 rgba(0, 0, 0, 0.1), 0 1px 2px 0 rgba(0, 0, 0, 0.06)");

        container.add(createToolbar());
        configureGrid();
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);
        container.add(grid);
        container.setFlexGrow(1, grid);

        return container;
    }

    private Component createToolbar() {
        HorizontalLayout toolbar = new HorizontalLayout();
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.CENTER);

        searchField.setPlaceholder("Rechercher...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setWidth("350px");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> filterExams(e.getValue()));

        Button filterAll = createFilterChip("Tous", null);
        Button filterProgress = createFilterChip("En cours", ExamStatus.IN_PROGRESS);

        toolbar.add(searchField, filterAll, filterProgress);
        Button refreshBtn = new Button(VaadinIcon.REFRESH.create(), e -> refreshGrid());
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        toolbar.add(refreshBtn);
        toolbar.expand(searchField);
        return toolbar;
    }

    private Button createFilterChip(String label, ExamStatus status) {
        Button btn = new Button(label);
        btn.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
        btn.getStyle().set("border-radius", "20px").set("border", "1px solid #6b7280");
        btn.addClickListener(e -> filterByStatus(status));
        return btn;
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.removeAllColumns();

        grid.addColumn(new ComponentRenderer<>(exam -> {
            HorizontalLayout row = new HorizontalLayout();
            row.setAlignItems(Alignment.CENTER);
            if (exam.getPatient() == null) return new Span("N/A");

            Avatar avatar = new Avatar(exam.getPatient().getFirstName() + " " + exam.getPatient().getLastName());
            avatar.getStyle().set("background-color", "#e0e7ff").set("color", "#3730a3");

            VerticalLayout info = new VerticalLayout();
            info.setPadding(false);
            info.setSpacing(false);
            Span name = new Span(exam.getPatient().getFirstName() + " " + exam.getPatient().getLastName());
            name.getStyle().set("font-weight", "600").set("font-size", "0.95em");
            Span ipp = new Span("IPP: " + exam.getPatient().getPatientId());
            ipp.getStyle().set("font-size", "0.8em").set("color", "gray");

            info.add(name, ipp);
            row.add(avatar, info);
            return row;
        })).setHeader("Patient").setWidth("250px").setFlexGrow(1);

        grid.addColumn(new ComponentRenderer<>(exam -> {
            VerticalLayout layout = new VerticalLayout();
            layout.setPadding(false);
            layout.setSpacing(false);
            Span acc = new Span(exam.getAccessionNumber());
            acc.getStyle().set("font-family", "monospace").set("font-weight", "bold");
            Span mod = new Span(exam.getModality());
            mod.getElement().getThemeList().add("badge contrast");
            layout.add(acc, mod);
            return layout;
        })).setHeader("Examen").setWidth("150px");

        grid.addColumn(exam -> exam.getScheduledDateTime() != null ?
                        exam.getScheduledDateTime().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")) : "-")
                .setHeader("Planifié").setWidth("120px");

        grid.addColumn(new ComponentRenderer<>(exam -> createStatusBadge(exam.getStatus())))
                .setHeader("État").setWidth("140px");

        grid.addColumn(new ComponentRenderer<>(exam -> {
            HorizontalLayout actions = new HorizontalLayout();

            Button reportBtn = new Button("Rapport", VaadinIcon.ARROW_RIGHT.create());
            reportBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            reportBtn.addClickListener(e -> openSplitViewDialog(exam));

            Button viewerBtn = new Button(VaadinIcon.EXPAND_SQUARE.create());
            viewerBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL);
            viewerBtn.setTooltipText("Voir images en plein écran");
            viewerBtn.addClickListener(e -> openFullscreenViewer(exam));

            actions.add(viewerBtn, reportBtn);
            return actions;
        })).setHeader("Actions").setWidth("200px").setFrozenToEnd(true);
    }

    // =================================================================
    // SECTION 3: ARCHIVES & PDF DOWNLOAD
    // =================================================================

    private VerticalLayout createHistoryContainer() {
        VerticalLayout container = new VerticalLayout();
        container.setSizeFull();
        container.setPadding(true);
        container.setSpacing(true);
        container.getStyle().set("background-color", "white").set("border-radius", "12px").set("box-shadow", "0 1px 3px 0 rgba(0, 0, 0, 0.1)");

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);

        H3 title = new H3("Archives des rapports");
        title.getStyle().set("margin", "0").set("color", "#4b5563");

        Button refreshBtn = new Button("Actualiser", VaadinIcon.REFRESH.create());
        refreshBtn.addClickListener(e -> refreshReportsGrid());

        header.add(title);
        header.setFlexGrow(1, title);
        header.add(refreshBtn);

        configureReportsGrid();
        reportsGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        container.add(header, reportsGrid);
        container.setFlexGrow(1, reportsGrid);
        return container;
    }

    private void configureReportsGrid() {
        reportsGrid.setSizeFull();
        reportsGrid.removeAllColumns();

        reportsGrid.addColumn(report -> {
            if (report.getExam() != null && report.getExam().getPatient() != null) {
                return report.getExam().getPatient().getLastName() + " " + report.getExam().getPatient().getFirstName();
            }
            return "N/A";
        }).setHeader("Patient").setSortable(true);

        reportsGrid.addColumn(report -> report.getExam() != null ? report.getExam().getModality() : "-")
                .setHeader("Mod").setWidth("80px");

        reportsGrid.addColumn(report -> {
            if (report.getAuthor() != null) {
                return "Dr. " + report.getAuthor().getFirstName() + " " + report.getAuthor().getLastName();
            }
            if (report.getRadiologue() != null) {
                return "Dr. " + report.getRadiologue().getFirstName() + " " + report.getRadiologue().getLastName();
            }
            return "Inconnu";
        }).setHeader("Radiologue").setWidth("200px");

        reportsGrid.addColumn(report -> report.getValidatedAt() != null ?
                report.getValidatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "").setHeader("Validé le");

        reportsGrid.addColumn(new ComponentRenderer<>(report -> {
            HorizontalLayout actions = new HorizontalLayout();

            // 1. Bouton PDF
            Button pdfBtn = new Button(VaadinIcon.DOWNLOAD.create());
            pdfBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            pdfBtn.setTooltipText("Télécharger PDF");

            Anchor downloadLink = new Anchor(createPdfResource(report), "");
            downloadLink.add(pdfBtn);
            downloadLink.getElement().setAttribute("download", true);

            // 2. Bouton Détails
            Button detailsBtn = new Button(VaadinIcon.EYE.create());
            detailsBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            detailsBtn.setTooltipText("Voir détails");
            detailsBtn.addClickListener(e -> openReportDialog(report));

            // 3. Bouton Viewer
            Button viewerBtn = new Button(VaadinIcon.EXPAND_SQUARE.create());
            viewerBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            viewerBtn.setTooltipText("Revoir images");
            viewerBtn.addClickListener(e -> openFullscreenViewer(report.getExam()));

            actions.add(downloadLink, detailsBtn, viewerBtn);
            return actions;
        })).setHeader("Actions").setWidth("220px");
    }

    // =================================================================
    // SECTION 4: ACTIONS, VIEWERS & PDF GENERATION
    // =================================================================

    // --- PDF ---
    private StreamResource createPdfResource(Report report) {
        String filename = "Rapport_" + report.getExam().getAccessionNumber() + ".pdf";
        return new StreamResource(filename, () -> generatePdfContent(report));
    }

    private ByteArrayInputStream generatePdfContent(Report report) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);

            Paragraph title = new Paragraph("COMPTE RENDU RADIOLOGIQUE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingAfter(20);

            PdfPCell cellPatient = new PdfPCell();
            cellPatient.setBorder(0);
            cellPatient.addElement(new Paragraph("PATIENT:", headerFont));
            if (report.getExam().getPatient() != null) {
                Patient p = report.getExam().getPatient();
                cellPatient.addElement(new Paragraph("Nom: " + p.getLastName() + " " + p.getFirstName(), normalFont));
                cellPatient.addElement(new Paragraph("IPP: " + p.getPatientId(), normalFont));
                cellPatient.addElement(new Paragraph("Né(e) le: " + (p.getDateOfBirth() != null ? p.getDateOfBirth().toString() : "N/A"), normalFont));
            }
            table.addCell(cellPatient);

            PdfPCell cellExam = new PdfPCell();
            cellExam.setBorder(0);
            cellExam.addElement(new Paragraph("EXAMEN:", headerFont));
            cellExam.addElement(new Paragraph("Modalité: " + report.getExam().getModality(), normalFont));
            cellExam.addElement(new Paragraph("Date: " + report.getExam().getScheduledDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), normalFont));
            cellExam.addElement(new Paragraph("Accession: " + report.getExam().getAccessionNumber(), normalFont));
            table.addCell(cellExam);

            document.add(table);
            document.add(new Paragraph("______________________________________________________________________________"));
            document.add(new Paragraph(" "));

            Paragraph pFindingsHeader = new Paragraph("OBSERVATIONS / COMPTE RENDU :", headerFont);
            pFindingsHeader.setSpacingAfter(5);
            document.add(pFindingsHeader);

            Paragraph pFindings = new Paragraph(report.getFindings(), normalFont);
            pFindings.setSpacingAfter(15);
            document.add(pFindings);

            Paragraph pConclusionHeader = new Paragraph("CONCLUSION :", headerFont);
            pConclusionHeader.setSpacingAfter(5);
            document.add(pConclusionHeader);

            Paragraph pConclusion = new Paragraph(report.getConclusion(), boldFont);
            pConclusion.setSpacingAfter(30);
            document.add(pConclusion);

            Paragraph signature = new Paragraph();
            signature.setAlignment(Element.ALIGN_RIGHT);
            signature.add(new Paragraph("Validé le: " + report.getValidatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")), normalFont));

            String docName = "Inconnu";
            if (report.getAuthor() != null) {
                docName = "Dr. " + report.getAuthor().getFirstName() + " " + report.getAuthor().getLastName();
            } else if (report.getRadiologue() != null) {
                docName = "Dr. " + report.getRadiologue().getFirstName() + " " + report.getRadiologue().getLastName();
            }

            signature.add(new Paragraph("Signé par: " + docName, boldFont));
            document.add(signature);

            document.close();
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // --- FULLSCREEN VIEWER ---
    private void openFullscreenViewer(Exam exam) {
        if (exam == null) {
            Notification.show("Examen invalide", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        // Vérifier que le StudyInstanceUID existe
        if (exam.getStudyInstanceUID() == null || exam.getStudyInstanceUID().isEmpty()) {
            Notification.show("Aucune image DICOM disponible pour cet examen", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        Dialog fullscreenDialog = new Dialog();
        fullscreenDialog.setHeaderTitle("Visualiseur DICOM - " + exam.getAccessionNumber());
        fullscreenDialog.setWidth("100vw");
        fullscreenDialog.setHeight("100vh");
        fullscreenDialog.setResizable(true);
        fullscreenDialog.setDraggable(false);
        fullscreenDialog.setModal(true);

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        content.setPadding(false);
        content.setSpacing(false);
        content.getStyle().set("background-color", "#000000");

        // Construire l'URL OHIF avec le StudyInstanceUID
        String studyUid = exam.getStudyInstanceUID();
        String url = ohifBaseUrl + "/viewer?StudyInstanceUIDs=" + studyUid;

        IFrame iframe = new IFrame(url);
        iframe.setSizeFull();
        iframe.getStyle()
                .set("border", "none")
                .set("display", "block");
        iframe.setAllow("fullscreen"); // Permettre le mode plein écran dans OHIF

        // Bouton de fermeture
        Button closeBtn = new Button("Fermer le Viewer", VaadinIcon.CLOSE.create());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_PRIMARY);
        closeBtn.addClickListener(e -> fullscreenDialog.close());

        // Bouton optionnel: Ouvrir dans un nouvel onglet
        Button openInTabBtn = new Button("Ouvrir dans un nouvel onglet", VaadinIcon.EXTERNAL_LINK.create());
        openInTabBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        openInTabBtn.addClickListener(e -> {
            getUI().ifPresent(ui -> ui.getPage().open(url, "_blank"));
        });

        // Barre d'outils
        HorizontalLayout topBar = new HorizontalLayout(openInTabBtn, closeBtn);
        topBar.setWidthFull();
        topBar.setPadding(true);
        topBar.setSpacing(true);
        topBar.setJustifyContentMode(JustifyContentMode.END);
        topBar.getStyle()
                .set("background-color", "#1e293b")
                .set("border-bottom", "1px solid #334155")
                .set("box-shadow", "0 2px 4px rgba(0, 0, 0, 0.1)");
        topBar.setHeight("60px");
        topBar.setAlignItems(Alignment.CENTER);

        content.add(topBar, iframe);
        content.setFlexGrow(1, iframe);

        fullscreenDialog.add(content);
        fullscreenDialog.open();
    }

    // --- SPLIT EDITOR ---
    private void openSplitViewDialog(Exam exam) {
        if (exam == null) return;

        Dialog splitDialog = new Dialog();
        splitDialog.setWidth("98vw");
        splitDialog.setHeight("95vh");
        splitDialog.setResizable(false);
        splitDialog.setDraggable(false);

        String patientInfo = (exam.getPatient() != null) ? exam.getPatient().getLastName() : "Patient";
        splitDialog.setHeaderTitle("Interprétation : " + patientInfo + " (" + exam.getModality() + ")");

        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(60);

        String studyUid = exam.getStudyInstanceUID() != null ? exam.getStudyInstanceUID() : "1.2.840.10008.5.1.4.1.1.1." + exam.getId();
        String url = ohifBaseUrl + "/ohif/viewer?StudyInstanceUIDs=" + studyUid;

        IFrame iframe = new IFrame(url);
        iframe.setSizeFull();
        iframe.getStyle().set("border", "none");

        VerticalLayout editorLayout = new VerticalLayout();
        editorLayout.setSizeFull();
        editorLayout.setPadding(true);
        editorLayout.getStyle().set("background-color", "#f9fafb");

        TextArea findings = new TextArea("Observations / Compte rendu");
        findings.setSizeFull();
        findings.setPlaceholder("Décrivez vos observations cliniques...");
        findings.getStyle().set("font-family", "monospace");

        TextArea conclusion = new TextArea("Conclusion");
        conclusion.setWidthFull();
        conclusion.setHeight("150px");
        conclusion.setPlaceholder("Synthèse et conclusion...");

        if(exam.getReport() != null) {
            findings.setValue(exam.getReport().getFindings() != null ? exam.getReport().getFindings() : "");
            conclusion.setValue(exam.getReport().getConclusion() != null ? exam.getReport().getConclusion() : "");
        }

        Button saveBtn = new Button("Valider et Signer", VaadinIcon.CHECK_CIRCLE.create());
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        saveBtn.getStyle().set("box-shadow", "0 2px 4px rgba(0,0,0,0.1)");

        saveBtn.addClickListener(e -> {
            Report report = exam.getReport();
            boolean isNew = (report == null);
            if (isNew) {
                report = new Report();
                report.setExam(exam);
                if (currentUser != null) {
                    report.setAuthor(currentUser);
                    report.setRadiologue(currentUser);
                }
                report.setCreatedAt(LocalDateTime.now());
                exam.setReport(report);
            }
            report.setFindings(findings.getValue());
            report.setConclusion(conclusion.getValue());
            report.setUpdatedAt(LocalDateTime.now());
            report.setValidated(true);
            report.setValidatedAt(LocalDateTime.now());
            exam.setStatus(ExamStatus.COMPLETED);
            reportRepository.save(report);
            examRepository.save(exam);

            splitDialog.close();
            Notification.show("Rapport validé").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            if (worklistContainer.isVisible()) refreshGrid();
            if (historyContainer.isVisible()) refreshReportsGrid();
        });

        Button closeBtn = new Button("Fermer", e -> splitDialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button maximizeBtn = new Button("Maximiser Image", VaadinIcon.EXPAND.create());
        maximizeBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        maximizeBtn.addClickListener(e -> openFullscreenViewer(exam));

        HorizontalLayout actions = new HorizontalLayout(maximizeBtn, closeBtn, saveBtn);
        actions.setWidthFull();
        actions.setJustifyContentMode(JustifyContentMode.END);
        actions.setAlignItems(Alignment.CENTER);

        editorLayout.add(findings, conclusion, actions);
        editorLayout.expand(findings);

        splitLayout.addToPrimary(iframe);
        splitLayout.addToSecondary(editorLayout);
        splitDialog.add(splitLayout);
        splitDialog.open();
    }

    // --- DETAILS DIALOG ---
    private void openReportDialog(Report report) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Détails du rapport");
        dialog.setWidth("800px");

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);

        HorizontalLayout auditLayout = new HorizontalLayout();
        auditLayout.setWidthFull();
        auditLayout.setAlignItems(Alignment.BASELINE);
        auditLayout.getStyle().set("background-color", "#f1f5f9").set("padding", "15px").set("border-radius", "8px").set("border", "1px solid #e2e8f0");

        String authorName = "Inconnu";
        if (report.getAuthor() != null) {
            authorName = "Dr. " + report.getAuthor().getFirstName() + " " + report.getAuthor().getLastName();
        } else if (report.getRadiologue() != null) {
            authorName = "Dr. " + report.getRadiologue().getFirstName() + " " + report.getRadiologue().getLastName();
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String createdStr = (report.getCreatedAt() != null) ? report.getCreatedAt().format(fmt) : "-";
        String updatedStr = (report.getUpdatedAt() != null) ? report.getUpdatedAt().format(fmt) : "-";

        TextField authorField = new TextField("Auteur / Signataire");
        authorField.setValue(authorName);
        authorField.setReadOnly(true);
        TextField createdField = new TextField("Date Création");
        createdField.setValue(createdStr);
        createdField.setReadOnly(true);
        TextField updatedField = new TextField("Dernière Modif.");
        updatedField.setValue(updatedStr);
        updatedField.setReadOnly(true);

        auditLayout.add(authorField, createdField, updatedField);

        TextArea findingsDisplay = new TextArea("Observations");
        findingsDisplay.setValue(report.getFindings() != null ? report.getFindings() : "");
        findingsDisplay.setReadOnly(true);
        findingsDisplay.setWidthFull();
        findingsDisplay.setHeight("200px");

        TextArea conclusionDisplay = new TextArea("Conclusion");
        conclusionDisplay.setValue(report.getConclusion() != null ? report.getConclusion() : "");
        conclusionDisplay.setReadOnly(true);
        conclusionDisplay.setWidthFull();

        Button closeBtn = new Button("Fermer", e -> dialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        content.add(auditLayout, findingsDisplay, conclusionDisplay, closeBtn);
        dialog.add(content);
        dialog.open();
    }

    private Span createStatusBadge(ExamStatus status) {
        String label = "Inconnu";
        String theme = "badge";
        if (status != null) {
            switch (status) {
                case COMPLETED: label = "Terminé"; theme = "badge success"; break;
                case IN_PROGRESS: label = "En cours"; theme = "badge"; break;
                case PLANNED: label = "Planifié"; theme = "badge contrast"; break;
                case CANCELLED: label = "Annulé"; theme = "badge error"; break;
            }
        }
        Span badge = new Span(label);
        badge.getElement().getThemeList().add(theme);
        return badge;
    }

    private void filterExams(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            grid.setItems(allExams);
        } else {
            String term = searchTerm.toLowerCase().trim();
            List<Exam> filtered = allExams.stream().filter(exam ->
                    exam.getAccessionNumber().toLowerCase().contains(term) ||
                            (exam.getPatient() != null &&
                                    (exam.getPatient().getLastName().toLowerCase().contains(term) ||
                                            exam.getPatient().getPatientId().toLowerCase().contains(term)))
            ).toList();
            grid.setItems(filtered);
        }
    }

    private void filterByStatus(ExamStatus status) {
        if (status == null) {
            grid.setItems(allExams);
        } else {
            List<Exam> filtered = allExams.stream().filter(e -> e.getStatus() == status).toList();
            grid.setItems(filtered);
        }
    }

    private void refreshGrid() {
        if (currentUser != null && currentUser.getRole() == UserRole.RADIOLOGUE) {
            allExams = examRepository.findAllWithRelations();
        } else {
            allExams = examRepository.findAllWithRelations();
        }
        grid.setItems(allExams);
    }

    private void refreshReportsGrid() {
        reportsGrid.setItems(reportRepository.findAllWithRelations());
    }
}