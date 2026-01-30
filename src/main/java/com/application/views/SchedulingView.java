package com.application.views;

import com.application.entity.Exam;
import com.application.entity.ExamStatus;
import com.application.entity.ModalityType;
import com.application.repository.ExamRepository;
import com.application.repository.ModalityTypeRepository;
import com.application.views.calendar.ExamCalendarView;
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
    private final ModalityTypeRepository modalityTypeRepo;
    private final Grid<Exam> examGrid = new Grid<>(Exam.class);
    private ExamCalendarView examCalendar;

    public SchedulingView(ExamRepository examRepo, ModalityTypeRepository modalityTypeRepo) {
        this.examRepo = examRepo;
        this.modalityTypeRepo = modalityTypeRepo;
        
        // Initialize calendar
        examCalendar = new ExamCalendarView(examRepo);
        
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        addClassName("scheduling-view");

        add(createHeader());
        add(createContent());
        
        refreshExamList();
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
                .set("border-radius", "0 0 16px 16px");

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
        refreshBtn.addClickListener(e -> refreshExamList());

        Button calendarBtn = new Button("Calendrier", VaadinIcon.CALENDAR.create());
        calendarBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        calendarBtn.getStyle()
                .set("background", "#7f1d1d !important")
                .set("color", "white !important")
                .set("font-weight", "bold")
                .set("border", "none !important");
        calendarBtn.addClickListener(e -> {
            Notification.show("Ouverture du calendrier...", 2000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            examCalendar.show();
        });

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
        
        content.add(examGrid);
        return content;
    }

    private void configureGrid() {
        examGrid.setSizeFull();
        examGrid.removeAllColumns();
        examGrid.addClassName("scheduling-grid");

        // Colonne Patient
        examGrid.addColumn(exam -> exam.getPatient() != null ?
                        exam.getPatient().getLastName() + " " + exam.getPatient().getFirstName() : "N/A")
                .setHeader("Patient")
                .setSortable(true)
                .setFlexGrow(1);

        // Colonne Modalité
        examGrid.addColumn(Exam::getModality)
                .setHeader("Modalité")
                .setWidth("100px")
                .setFlexGrow(0);

        // Colonne Statut
        examGrid.addColumn(exam -> {
            Span statusBadge = new Span(exam.getStatus() != null ? exam.getStatus().toString() : "N/A");
            statusBadge.getElement().getThemeList().add("badge");

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
            return statusBadge;
        })
                .setHeader("Statut")
                .setWidth("120px")
                .setFlexGrow(0);

        // Colonne Date programmée
        examGrid.addColumn(exam -> exam.getScheduledDateTime() != null ?
                        exam.getScheduledDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "Non programmé")
                .setHeader("Date programmée")
                .setWidth("160px")
                .setFlexGrow(0);

        // Colonne Actions
        examGrid.addComponentColumn(this::createActionButtons)
                .setHeader("Actions")
                .setWidth("200px")
                .setFlexGrow(0);
    }

    private Component createActionButtons(Exam exam) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);

        if (exam.getStatus() == ExamStatus.CREATED) {
            Button scheduleBtn = new Button("Planifier", VaadinIcon.CALENDAR.create());
            scheduleBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
            scheduleBtn.addClickListener(e -> openScheduleDialog(exam));
            actions.add(scheduleBtn);
        }

        Button detailsBtn = new Button(VaadinIcon.INFO_CIRCLE.create());
        detailsBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        detailsBtn.getElement().setProperty("title", "Voir les détails");
        detailsBtn.addClickListener(e -> showExamDetails(exam));
        actions.add(detailsBtn);

        return actions;
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
        
        // Sélecteur de modalité
        ComboBox<ModalityType> modalitySelector = new ComboBox<>("Modalité");
        modalitySelector.setItems(modalityTypeRepo.findAllActiveOrdered());
        modalitySelector.setItemLabelGenerator(modality -> modality.getCode() + " - " + modality.getName());
        modalitySelector.setPlaceholder("Sélectionner une modalité");
        modalitySelector.setWidthFull();
        
        // Pré-sélectionner la modalité actuelle si elle existe
        if (exam.getModality() != null) {
            modalityTypeRepo.findByCode(exam.getModality()).ifPresent(modalitySelector::setValue);
        }
        
        content.add(modalitySelector);
        
        // DateTimePicker pour la date
        DateTimePicker dateTimePicker = new DateTimePicker("Date et heure programmée");
        dateTimePicker.setValue(exam.getScheduledDateTime() != null ? 
            exam.getScheduledDateTime() : LocalDateTime.now().plusDays(1));
        dateTimePicker.setWidthFull();
        content.add(dateTimePicker);
        
        dialog.add(content);
        dialog.setCancelable(true);
        dialog.setConfirmText("Planifier");
        dialog.setConfirmButtonTheme("primary");
        
        dialog.addConfirmListener(e -> {
            if (dateTimePicker.getValue() != null && modalitySelector.getValue() != null) {
                exam.setScheduledDateTime(dateTimePicker.getValue());
                exam.setModality(modalitySelector.getValue().getCode());
                exam.setStatus(ExamStatus.PLANNED);
                examRepo.save(exam);
                
                Notification.show(
                    "Examen planifié avec succès pour le " + 
                    dateTimePicker.getValue().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) +
                    " avec la modalité " + modalitySelector.getValue().getCode(),
                    3000,
                    Notification.Position.BOTTOM_END
                ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                
                refreshExamList();
            } else {
                if (modalitySelector.getValue() == null) {
                    Notification.show("Veuillez sélectionner une modalité", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                } else {
                    Notification.show("Veuillez sélectionner une date et heure", 3000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            }
        });
        
        dialog.open();
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
        content.add(new Span("Modalité: " + exam.getModality()));
        content.add(new Span("Statut: " + (exam.getStatus() != null ? exam.getStatus().toString() : "N/A")));
        content.add(new Span("Date programmée: " + (exam.getScheduledDateTime() != null ?
            exam.getScheduledDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "Non programmé")));
        content.add(new Span("Priorité: " + (exam.getPriority() != null ? exam.getPriority().toString() : "NORMAL")));
        content.add(new Span("Médecin: " + (exam.getMedecin() != null ?
            exam.getMedecin().getFirstName() + " " + exam.getMedecin().getLastName() : "N/A")));
        
        dialog.add(content);
        dialog.setConfirmText("Fermer");
        dialog.setCancelButton(null);
        
        dialog.open();
    }

    private void refreshExamList() {
        List<Exam> exams = examRepo.findByStatusWithRelations(ExamStatus.CREATED);
        
        examGrid.setItems(exams);
    }
}
