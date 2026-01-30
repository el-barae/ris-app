package com.application.views;

import com.application.entity.Exam;
import com.application.entity.ExamStatus;
import com.application.entity.Priority;
import com.application.entity.ProcedureCatalog;
import com.application.entity.ModalityType;
import com.application.repository.ExamRepository;
import com.application.repository.ProcedureCatalogRepository;
import com.application.repository.ModalityTypeRepository;
import com.application.service.OrthancWorklistService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import com.vaadin.flow.component.page.Page;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Route(value = "worklist-dragdrop", layout = MainLayout.class)
@PageTitle("Worklist DICOM (MWL)")
@RolesAllowed({"ADMIN", "TECHNICIEN"})
public class WorklistDragDropView extends VerticalLayout {

    private final ExamRepository examRepo;
    private final OrthancWorklistService orthancWorklistService;
    private final ProcedureCatalogRepository procedureRepo;
    private final ModalityTypeRepository modalityRepo;
    
    private final Grid<Exam> rightGrid = new Grid<>(Exam.class);
    private final VerticalLayout leftCardsContainer = new VerticalLayout();
    private final TextField searchField = new TextField();
    private final Span plannedCount;
    private final Span inProgressCount;

    private List<Exam> allPlannedExams = new ArrayList<>();
    private List<Exam> draggedItems = new ArrayList<>();
    
    // Pour les notifications WebSocket
    private Page page;

    public WorklistDragDropView(ExamRepository examRepo, OrthancWorklistService orthancWorklistService, 
                                ProcedureCatalogRepository procedureRepo, ModalityTypeRepository modalityRepo) {
        this.examRepo = examRepo;
        this.orthancWorklistService = orthancWorklistService;
        this.procedureRepo = procedureRepo;
        this.modalityRepo = modalityRepo;
        this.plannedCount = new Span();
        this.inProgressCount = new Span();
        this.plannedCount.setText("0");
        this.inProgressCount.setText("0");
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("worklist-view");

        // Header principal
        add(createMainHeader());

        // Configuration des grilles
        configureCardsContainer();
        configureGrid(rightGrid);

        // Configuration du Drag & Drop
        setupDragAndDrop();

        // Mise en page (Layout Split)
        add(createSplitLayout());

        refreshGrids();
    }

    private Component createMainHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setPadding(true);
        header.setSpacing(true);
        header.setAlignItems(Alignment.CENTER);
        header.getStyle()
                .set("background", "linear-gradient(135deg, #10b981 0%, #059669 100%)")
                .set("color", "white")
                .set("box-shadow", "0 4px 6px rgba(0,0,0,0.1)")
                .set("border-radius", "0 0 16px 16px");

        // Icône et titre
        HorizontalLayout titleLayout = new HorizontalLayout();
        titleLayout.setAlignItems(Alignment.CENTER);
        titleLayout.setSpacing(true);

        Icon worklistIcon = VaadinIcon.LIST.create();
        worklistIcon.setSize("32px");
        worklistIcon.getStyle().set("color", "white");

        H2 title = new H2("Worklist DICOM");
        title.getStyle()
                .set("margin", "0")
                .set("color", "white")
                .set("font-weight", "600");

        titleLayout.add(worklistIcon, title);

        // Badge pour le nombre d'examens planifiés
        plannedCount.getStyle()
                .set("background-color", "rgba(255,255,255,0.2)")
                .set("color", "white")
                .set("padding", "0.5rem 1rem")
                .set("border-radius", "2rem")
                .set("font-weight", "600")
                .set("font-size", "14px");

        // Badge pour le nombre d'examens en cours
        inProgressCount.getStyle()
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
            refreshGrids();
            Notification.show("Liste actualisée", 2000, Notification.Position.BOTTOM_END);
        });

        // Bouton exporter MWL
//        Button exportBtn = new Button("Exporter MWL", VaadinIcon.DOWNLOAD.create());
//        exportBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
//        exportBtn.getStyle()
//                .set("background", "rgba(255,255,255,0.2)")
//                .set("color", "white");
//        exportBtn.addClickListener(e -> exportMWL());

        // Bouton envoyer à Orthanc
        Button sendToOrthancBtn = new Button("Envoyer à MWL", VaadinIcon.PLAY.create());
        sendToOrthancBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendToOrthancBtn.getStyle()
                .set("background", "#7f1d1d !important")
                .set("color", "white !important")
                .set("border", "none !important");
        sendToOrthancBtn.addClickListener(e -> sendToOrthanc());

        header.add(titleLayout, plannedCount, inProgressCount, refreshBtn, sendToOrthancBtn);
        header.setFlexGrow(1, titleLayout);
        header.setFlexGrow(0, plannedCount);
        header.setFlexGrow(0, inProgressCount);

        return header;
    }

    private Component createSplitLayout() {
        // Layout Gauche - Examens en attente
        VerticalLayout leftLayout = new VerticalLayout();
        leftLayout.setSizeFull();
        leftLayout.setPadding(false);
        leftLayout.setSpacing(false);

        // Header gauche avec recherche
        HorizontalLayout leftHeader = new HorizontalLayout();
        leftHeader.setWidthFull();
        leftHeader.setPadding(true);
        leftHeader.setSpacing(true);
        leftHeader.setAlignItems(Alignment.CENTER);
        leftHeader.getStyle()
                .set("background-color", "#f5f5f5")
                .set("border-bottom", "2px solid #6b7280");

        H3 leftTitle = new H3(" Examens en Attente");
        leftTitle.getStyle()
                .set("margin", "0")
                .set("color", "#374151");

        // Barre de recherche
        searchField.setPlaceholder("Rechercher un patient, modalité...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setWidth("300px");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setValueChangeTimeout(300);
        searchField.addValueChangeListener(e -> filterPlannedExams(e.getValue()));
        searchField.getStyle()
                .set("--lumo-contrast-10pct", "transparent");

        leftHeader.add(leftTitle, searchField);
        leftHeader.setFlexGrow(1, leftTitle);

        // Scroll container pour les cartes
        Scroller leftScroller = new Scroller(leftCardsContainer);
        leftScroller.setSizeFull();
        leftScroller.getStyle()
                .set("background-color", "#f5f5f5")
                .set("padding", "1rem");

        leftLayout.add(leftHeader, leftScroller);

        // Layout Droite - Worklist Active
        VerticalLayout rightLayout = new VerticalLayout();
        rightLayout.setSizeFull();
        rightLayout.setPadding(false);
        rightLayout.setSpacing(false);

        // Header droite
        HorizontalLayout rightHeader = new HorizontalLayout();
        rightHeader.setWidthFull();
        rightHeader.setPadding(true);
        rightHeader.setSpacing(true);
        rightHeader.setAlignItems(Alignment.CENTER);
        rightHeader.getStyle()
                .set("background-color", "#f5f5f5")
                .set("border-bottom", "2px solid #10b981");

        H3 rightTitle = new H3(" Examens a envoye vers MWL");
        rightTitle.getStyle()
                .set("margin", "0")
                .set("color", "#374151");

        Span activeBadge = new Span(String.valueOf(rightGrid.getListDataView().getItemCount()));
        activeBadge.getStyle()
                .set("background-color", "#10b981")
                .set("color", "white")
                .set("padding", "0.25rem 0.75rem")
                .set("border-radius", "1rem")
                .set("font-weight", "600")
                .set("font-size", "13px");

        Button clearAllBtn = new Button("Tout retirer", VaadinIcon.TRASH.create());
        clearAllBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        clearAllBtn.addClickListener(e -> clearAllFromWorklist());

        rightHeader.add(rightTitle, activeBadge);
        rightHeader.setFlexGrow(1, rightTitle);
        rightHeader.add(clearAllBtn);

        Div rightGridContainer = new Div(rightGrid);
        rightGridContainer.setSizeFull();
        rightGridContainer.getStyle()
                .set("background-color", "#f5f5f5")
                .set("padding", "1rem");

        rightLayout.add(rightHeader, rightGridContainer);

        // Split Layout
        SplitLayout splitLayout = new SplitLayout(leftLayout, rightLayout);
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(50);
        splitLayout.getStyle().set("box-shadow", "inset 0 0 10px rgba(0,0,0,0.05)");

        return splitLayout;
    }

    private void configureCardsContainer() {
        leftCardsContainer.setSizeFull();
        leftCardsContainer.setSpacing(true);
        leftCardsContainer.setPadding(false);
    }

    private Card createExamCard(Exam exam) {
        Card card = new Card();
        card.setWidth("100%");
        card.getStyle()
                .set("cursor", "grab")
                .set("transition", "all 0.3s ease")
                .set("border", "1px solid #6b7280")
                .set("border-radius", "12px")
                .set("box-shadow", "0 2px 4px rgba(0,0,0,0.05)")
                .set("background", "white");

        // Effet hover
        card.getElement().executeJs(
                "this.addEventListener('mouseenter', () => {" +
                        "  this.style.transform = 'translateY(-4px)';" +
                        "  this.style.boxShadow = '0 8px 16px rgba(0,0,0,0.1)';" +
                        "});" +
                        "this.addEventListener('mouseleave', () => {" +
                        "  this.style.transform = 'translateY(0)';" +
                        "  this.style.boxShadow = '0 2px 4px rgba(0,0,0,0.05)';" +
                        "});"
        );

        VerticalLayout cardContent = new VerticalLayout();
        cardContent.setSpacing(true);
        cardContent.setPadding(true);
        cardContent.getStyle().set("gap", "0.75rem");

        // En-tête de la carte avec icône
        HorizontalLayout cardHeader = new HorizontalLayout();
        cardHeader.setWidthFull();
        cardHeader.setAlignItems(Alignment.CENTER);

        Icon modalityIcon = getModalityIcon(exam.getModality());
        modalityIcon.setSize("24px");
        modalityIcon.getStyle().set("color", "#374151");

        Span patientName = new Span(exam.getPatient() != null ?
                exam.getPatient().getLastName() + " " + exam.getPatient().getFirstName() : "N/A");
        patientName.getStyle()
                .set("font-weight", "700")
                .set("font-size", "16px")
                .set("color", "#374151");

        cardHeader.add(modalityIcon, patientName);
        cardHeader.setFlexGrow(1, patientName);

        // Informations détaillées
        Div infoGrid = new Div();
        infoGrid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "1fr 1fr")
                .set("gap", "0.5rem")
                .set("font-size", "13px");

        infoGrid.add(
                createInfoItem("", "Modalité", exam.getModality()),
                createInfoItem("", "", exam.getScheduledDateTime() != null ?
                        exam.getScheduledDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A")
        );

        // Badge priorité
        if (exam.getPriority() != null && exam.getPriority() != Priority.NORMAL) {
            Span priorityBadge = new Span(exam.getPriority().toString());
            priorityBadge.getStyle()
                    .set("background-color", exam.getPriority() == Priority.URGENT ? "#7f1d1d" : "#374151")
                    .set("color", "white")
                    .set("font-size", "11px")
                    .set("padding", "0.25rem 0.5rem")
                    .set("border-radius", "0.5rem")
                    .set("font-weight", "600");
            cardHeader.add(priorityBadge);
        }

        // Boutons d'action
        HorizontalLayout actions = new HorizontalLayout();
        actions.setWidthFull();
        actions.setSpacing(true);
        actions.getStyle().set("margin-top", "0.5rem");

        Button addBtn = new Button(VaadinIcon.ARROW_RIGHT.create());
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        addBtn.setWidth("20%");
        addBtn.getStyle()
                .set("background", "linear-gradient(135deg, #10b981 0%, #059669 100%)")
                .set("border", "none");
        addBtn.addClickListener(e -> addToWorklist(exam));

        Button editBtn = new Button(VaadinIcon.EDIT.create());
        editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        editBtn.getElement().setProperty("title", "Modifier l'examen");
        editBtn.addClickListener(e -> openEditExamDialog(exam));

        Button deleteBtn = new Button(VaadinIcon.TRASH.create());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        deleteBtn.getElement().setProperty("title", "Supprimer l'examen");
        deleteBtn.addClickListener(e -> deleteExam(exam));

        Button infoBtn = new Button(VaadinIcon.INFO_CIRCLE.create());
        infoBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        infoBtn.addClickListener(e -> showExamDetails(exam));

        actions.add(addBtn, editBtn, deleteBtn, infoBtn);
        actions.setFlexGrow(1, addBtn);

        cardContent.add(cardHeader, infoGrid, actions);
        card.add(cardContent);

        // Rendre la carte draggable
        card.getElement().setProperty("draggable", "true");
        card.getElement().addEventListener("dragstart", e -> {
            draggedItems = new ArrayList<>(List.of(exam));
            card.getStyle().set("opacity", "0.5");
        });
        card.getElement().addEventListener("dragend", e -> {
            card.getStyle().set("opacity", "1");
        });

        return card;
    }

    private Icon getModalityIcon(String modality) {
        return switch (modality) {
            case "CT" -> VaadinIcon.DISC.create();
            case "MR", "IRM" -> VaadinIcon.MAGNET.create();
            case "US" -> VaadinIcon.VOLUME.create(); // ultrasound
            case "XR", "CR", "DX" -> VaadinIcon.RECORDS.create();
            default -> VaadinIcon.FILE.create(); // fallback
        };
    }

    private Div createInfoItem(String emoji, String label, String value) {
        Div item = new Div();
        item.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "0.5rem");

        Span emojiSpan = new Span(emoji);
        Span labelSpan = new Span(label + ":");
        labelSpan.getStyle()
                .set("color", "#6b7280")
                .set("font-weight", "500");
        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("color", "#1e293b")
                .set("font-weight", "600");

        item.add(emojiSpan, labelSpan, valueSpan);
        return item;
    }

    private void configureGrid(Grid<Exam> grid) {
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.addClassName("worklist-grid");
        grid.getStyle()
                .set("border-radius", "12px")
                .set("overflow", "hidden");

        // Colonne Patient
        grid.addColumn(exam -> exam.getPatient() != null ?
                        exam.getPatient().getLastName() + " " + exam.getPatient().getFirstName() : "N/A")
                .setHeader("Nom")
                .setSortable(true)
                .setFlexGrow(1);

        // Colonne Modalité
        grid.addColumn(Exam::getModality)
                .setHeader("Modalité")
                .setWidth("100px")
                .setFlexGrow(0);

        // Colonne Statut
        grid.addColumn(exam -> exam.getStatus() != null ? exam.getStatus().toString() : "N/A")
                .setHeader("Statut")
                .setWidth("120px")
                .setFlexGrow(0);

        // Colonne Date
        grid.addColumn(exam -> exam.getScheduledDateTime() != null ?
                        exam.getScheduledDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A")
                .setHeader("Date programmée")
                .setWidth("160px")
                .setFlexGrow(0);

        // Colonne Actions
        grid.addComponentColumn(this::createGridActions)
                .setHeader("Actions")
                .setWidth("120px")
                .setFlexGrow(0);

        // Activer le Drop sur la grille
        grid.setDropMode(GridDropMode.ON_GRID);
        grid.setRowsDraggable(true);
    }

    private Component createGridActions(Exam exam) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);

        Button removeBtn = new Button(VaadinIcon.CLOSE_CIRCLE.create());
        removeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        removeBtn.getElement().setProperty("title", "Retirer de la worklist");
        removeBtn.addClickListener(e -> removeFromWorklist(exam));

        Button infoBtn = new Button(VaadinIcon.INFO_CIRCLE.create());
        infoBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        infoBtn.getElement().setProperty("title", "Voir les détails");
        infoBtn.addClickListener(e -> showExamDetails(exam));

        actions.add(removeBtn, infoBtn);
        return actions;
    }

    private void setupDragAndDrop() {
        // Drop sur la grille droite
        rightGrid.addDropListener(event -> {
            if (!draggedItems.isEmpty()) {
                addToWorklist(draggedItems);
                draggedItems.clear();
            }
        });

        rightGrid.addDragStartListener(event -> {
            draggedItems = event.getDraggedItems();
        });
    }

    private void addToWorklist(Exam exam) {
        addToWorklist(List.of(exam));
    }

    private void addToWorklist(List<Exam> exams) {
        for (Exam exam : exams) {
            exam.setStatus(ExamStatus.SELECTED);
        }
        examRepo.saveAll(exams);
        
        // Rafraîchir les grilles après l'ajout
        refreshGrids();
        
        // Notification de succès
        Notification.show(exams.size() + " examen(s) ajouté(s) à la worklist", 3000, Notification.Position.BOTTOM_END);
    }

    private void removeFromWorklist(Exam exam) {
        exam.setStatus(ExamStatus.PLANNED);
        examRepo.save(exam);
        
        // Rafraîchir les grilles après la suppression
        refreshGrids();
        
        // Notification de succès
        Notification.show("Examen retiré de la worklist", 2000, Notification.Position.BOTTOM_END);
    }

    private void clearAllFromWorklist() {
        List<Exam> activeExams = examRepo.findByStatusWithRelations(ExamStatus.SELECTED);

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirmation");
        dialog.setText("Voulez-vous vraiment retirer tous les examens de la worklist ?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Confirmer");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            for (Exam exam : activeExams) {
                exam.setStatus(ExamStatus.PLANNED);
            }
            examRepo.saveAll(activeExams);
            
            // Rafraîchir les grilles après la suppression
            refreshGrids();
            
            // Notification de succès
            Notification.show(
                    activeExams.size() + " examen(s) retiré(s) de la worklist",
                    3000,
                    Notification.Position.BOTTOM_END
            );
        });
        dialog.open();
    }

    private void showExamDetails(Exam exam) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(" Détails de l'examen");
        dialog.setWidth("500px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);

        content.add(
                createDetailRow("", "Patient", exam.getPatient() != null ?
                        exam.getPatient().getLastName() + " " + exam.getPatient().getFirstName() : "N/A"),
                createDetailRow("", "IPP", exam.getPatient() != null ? exam.getPatient().getPatientId() : "N/A"),
                createDetailRow("", "CIN", exam.getPatient() != null ? exam.getPatient().getCin() : "N/A"),
                createDetailRow("", "Accession", exam.getAccessionNumber()),
                createDetailRow("", "Modalité", exam.getModality()),
                createDetailRow("", "Type", exam.getExamType() != null ? exam.getExamType().toString() : "N/A"),
                createDetailRow("", "Programmé", exam.getScheduledDateTime() != null ?
                        exam.getScheduledDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/A"),
                createDetailRow("", "Priorité", exam.getPriority() != null ? exam.getPriority().toString() : "NORMAL"),
                createDetailRow("", "Médecin", exam.getMedecin() != null ?
                        exam.getMedecin().getFirstName() + " " + exam.getMedecin().getLastName() : "N/A")
        );

        dialog.add(content);

        Button closeBtn = new Button("Fermer", e -> dialog.close());
        closeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        dialog.getFooter().add(closeBtn);

        dialog.open();
    }

    private void filterPlannedExams(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            displayPlannedExams(allPlannedExams);
        } else {
            String term = searchTerm.toLowerCase().trim();
            List<Exam> filtered = allPlannedExams.stream()
                    .filter(exam ->
                            (exam.getPatient() != null &&
                                    (exam.getPatient().getFirstName().toLowerCase().contains(term) ||
                                            exam.getPatient().getLastName().toLowerCase().contains(term) ||
                                            exam.getPatient().getPatientId().toLowerCase().contains(term))) ||
                                    exam.getModality().toLowerCase().contains(term) ||
                                    exam.getAccessionNumber().toLowerCase().contains(term)
                    )
                    .toList();
            displayPlannedExams(filtered);
        }
    }

    private Div createDetailRow(String emoji, String label, String value) {
        Div row = new Div();
        row.getStyle()
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("padding", "0.75rem")
                .set("border-bottom", "1px solid #6b7280");

        Span labelSpan = new Span(label + ":");
        labelSpan.getStyle()
                .set("font-weight", "600")
                .set("color", "#6b7280");

        Span valueSpan = new Span(value);
        valueSpan.getStyle()
                .set("color", "#1e293b")
                .set("font-weight", "600");

        row.add(labelSpan, valueSpan);
        return row;
    }

    private void refreshGrids() {
        // Charger les examens planifiés
        allPlannedExams = examRepo.findByStatusWithRelations(ExamStatus.PLANNED);
        displayPlannedExams(allPlannedExams);
        plannedCount.setText(String.valueOf(allPlannedExams.size()));

        // Charger les examens actifs
        List<Exam> activeExams = examRepo.findByStatusWithRelations(ExamStatus.SELECTED);
        rightGrid.setItems(activeExams);
        inProgressCount.setText(String.valueOf(activeExams.size()));
    }

    private void exportMWL() {
        // Logique d'exportation MWL à implémenter
        Notification.show("Exportation MWL non implémentée", 3000, Notification.Position.BOTTOM_END);
    }

    private void sendToOrthanc() {
        List<Exam> selectedExams = examRepo.findByStatusWithRelations(ExamStatus.SELECTED);
        
        if (selectedExams.isEmpty()) {
            Notification.show("Aucun examen sélectionné à envoyer", 3000, Notification.Position.BOTTOM_END);
            return;
        }

        // Confirmation dialog
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirmation");
        dialog.setText("Voulez-vous envoyer " + selectedExams.size() + " examen(s) à Orthanc ?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Envoyer");
        dialog.setConfirmButtonTheme("success primary");
        
        dialog.addConfirmListener(e -> {
            boolean success = orthancWorklistService.sendWorklistToOrthanc(selectedExams);
            
            if (success) {
                Notification.show(
                    selectedExams.size() + " examen(s) envoyé(s) avec succès à Orthanc",
                    3000,
                    Notification.Position.BOTTOM_END
                );
            } else {
                Notification.show(
                    "Erreur lors de l'envoi à Orthanc. Vérifiez la connexion et les logs.",
                    5000,
                    Notification.Position.BOTTOM_END
                );
            }
        });
        
        dialog.open();
    }

    private void displayPlannedExams(List<Exam> exams) {
        leftCardsContainer.removeAll();

        if (exams.isEmpty()) {
            Div emptyState = new Div();
            emptyState.getStyle()
                    .set("text-align", "center")
                    .set("padding", "4rem 2rem")
                    .set("color", "#6b7280");

            Icon emptyIcon = VaadinIcon.INBOX.create();
            emptyIcon.setSize("64px");

            H3 emptyTitle = new H3("Aucun examen en attente");
            emptyTitle.getStyle().set("color", "#6b7280");

            emptyState.add(emptyIcon, emptyTitle);
            leftCardsContainer.add(emptyState);
            return;
        }

        HorizontalLayout currentRow = null;

        for (int i = 0; i < exams.size(); i++) {
            if (i % 2 == 0) {
                currentRow = new HorizontalLayout();
                currentRow.setWidthFull();
                currentRow.setSpacing(true);
                leftCardsContainer.add(currentRow);
            }

            Card card = createExamCard(exams.get(i));
            card.setWidth("48%");
            if (currentRow != null) {
                currentRow.add(card);
            }
        }
    }

    private void openEditExamDialog(Exam exam) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Modifier l'examen");
        dialog.setWidth("600px");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);

        // Patient information (read-only)
        TextField patientField = new TextField("Patient");
        patientField.setReadOnly(true);
        patientField.setWidthFull();
        if (exam.getPatient() != null) {
            patientField.setValue(exam.getPatient().getLastName() + " " + exam.getPatient().getFirstName());
        }

        // Modality selection
        ComboBox<ModalityType> modalityCombo = new ComboBox<>("Modalité");
        modalityCombo.setWidthFull();
        modalityCombo.setItems(modalityRepo.findAllActiveOrdered());
        modalityCombo.setItemLabelGenerator(modality -> 
            modality.getCode() + " - " + modality.getName());
        
        // Find current modality
        if (exam.getModality() != null) {
            modalityRepo.findByCode(exam.getModality())
                .ifPresent(modalityCombo::setValue);
        }

        // Procedure selection
        ComboBox<ProcedureCatalog> procedureCombo = new ComboBox<>("Procédure");
        procedureCombo.setWidthFull();
        procedureCombo.setItemLabelGenerator(procedure -> 
            procedure.getName() + " (" + procedure.getProcedureCode() + ")");
        
        // Update procedures based on selected modality
        modalityCombo.addValueChangeListener(event -> {
            ModalityType selectedModality = event.getValue();
            if (selectedModality != null) {
                procedureCombo.setItems(procedureRepo.findByModalityTypeAndIsActive(selectedModality, true));
            } else {
                procedureCombo.setItems();
            }
            procedureCombo.clear();
        });

        // Set current procedure if exists
        if (exam.getProcedure() != null) {
            procedureCombo.setValue(exam.getProcedure());
            // Also set the modality
            if (exam.getProcedure().getModalityType() != null) {
                modalityCombo.setValue(exam.getProcedure().getModalityType());
            }
        }

        // Load initial procedures if modality is already set
        if (modalityCombo.getValue() != null) {
            procedureCombo.setItems(procedureRepo.findByModalityTypeAndIsActive(modalityCombo.getValue(), true));
        }

        content.add(patientField, modalityCombo, procedureCombo);
        dialog.add(content);

        // Buttons
        Button saveBtn = new Button("Enregistrer", e -> {
            ModalityType selectedModality = modalityCombo.getValue();
            ProcedureCatalog selectedProcedure = procedureCombo.getValue();

            if (selectedModality == null) {
                Notification.show("Veuillez sélectionner une modalité", 3000, Notification.Position.MIDDLE);
                return;
            }

            // Update exam
            exam.setModality(selectedModality.getCode());
            exam.setProcedure(selectedProcedure);
            
            // Update examType based on modality
            String modalityCode = selectedModality.getCode();
            switch (modalityCode) {
                case "CT" -> exam.setExamType(com.application.entity.ExamType.CT);
                case "MR" -> exam.setExamType(com.application.entity.ExamType.MRI);
                case "XR", "CR", "DX" -> exam.setExamType(com.application.entity.ExamType.RX);
                case "US" -> exam.setExamType(com.application.entity.ExamType.ECHO);
                case "MG" -> exam.setExamType(com.application.entity.ExamType.MAMMO);
                case "RF" -> exam.setExamType(com.application.entity.ExamType.FLUORO);
                case "PT" -> exam.setExamType(com.application.entity.ExamType.PET);
            }

            examRepo.save(exam);
            refreshGrids();
            
            Notification.show("Examen modifié avec succès", 3000, Notification.Position.BOTTOM_END);
            dialog.close();
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Annuler", e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout buttons = new HorizontalLayout(saveBtn, cancelBtn);
        buttons.setSpacing(true);
        dialog.getFooter().add(buttons);

        dialog.open();
    }

    private void deleteExam(Exam exam) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Confirmation de suppression");
        dialog.setText("Voulez-vous vraiment supprimer définitivement cet examen ?\n\n" +
                "Patient: " + (exam.getPatient() != null ? 
                        exam.getPatient().getLastName() + " " + exam.getPatient().getFirstName() : "N/A") + "\n" +
                "Modalité: " + exam.getModality() + "\n" +
                "Accession: " + exam.getAccessionNumber());
        dialog.setCancelable(true);
        dialog.setConfirmText("Supprimer");
        dialog.setConfirmButtonTheme("error primary");
        
        dialog.addConfirmListener(e -> {
            try {
                examRepo.delete(exam);
                refreshGrids();
                
                Notification.show(
                    "Examen supprimé définitivement",
                    3000,
                    Notification.Position.BOTTOM_END
                );
            } catch (Exception ex) {
                Notification.show(
                    "Erreur lors de la suppression: " + ex.getMessage(),
                    5000,
                    Notification.Position.BOTTOM_END
                );
            }
        });
        
        dialog.open();
    }
}