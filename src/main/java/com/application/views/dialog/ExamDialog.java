package com.application.views.dialog;

import com.application.entity.*;
import com.application.service.PatientService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.StringLengthValidator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.function.Consumer;

/**
 * Dialogue pour créer ou modifier un examen
 */
public class ExamDialog extends Dialog {
    
    private final Exam exam;
    private final Consumer<Exam> saveCallback;
    private final PatientService patientService;
    private final Binder<Exam> binder;
    
    // Champs du formulaire
    private final ComboBox<Patient> patient = new ComboBox<>("Patient");
    private final ComboBox<ExamType> examType = new ComboBox<>("Type d'examen");
    private final TextField modality = new TextField("Modalité");
    private final DatePicker scheduledDate = new DatePicker("Date programmée");
    private final TimePicker scheduledTime = new TimePicker("Heure programmée");
    private final ComboBox<Priority> priority = new ComboBox<>("Priorité");
    private final ComboBox<ExamStatus> status = new ComboBox<>("Statut");
    private final TextArea instructions = new TextArea("Instructions");
    
    public ExamDialog(Exam exam, Consumer<Exam> saveCallback, PatientService patientService) {
        this.exam = exam != null ? exam : new Exam();
        this.saveCallback = saveCallback;
        this.patientService = patientService;
        this.binder = new Binder<>(Exam.class);
        
        configureDialog();
        configureFields();
        configureBinder();
        bindFields();
        
        if (exam != null) {
            binder.setBean(exam);
            
            // Initialiser les champs date et heure si l'examen a une date programmée
            if (exam.getScheduledDateTime() != null) {
                scheduledDate.setValue(exam.getScheduledDateTime().toLocalDate());
                scheduledTime.setValue(exam.getScheduledDateTime().toLocalTime());
            }
            
            // Initialiser la modalité si le type d'examen est défini
            if (exam.getExamType() != null) {
                updateModality(exam.getExamType());
            }
        }
    }
    
    private void configureDialog() {
        setModal(true);
        setDraggable(true);
        setResizable(true);
        setWidth("700px");
        
        // Header
        H3 title = new H3(exam != null ? "Modifier l'examen" : "Nouvel examen");
        title.addClassNames("mb-m", "text-primary");
        
        // Formulaire
        FormLayout formLayout = new FormLayout();
        formLayout.setWidthFull();
        formLayout.add(patient, examType, modality, scheduledDate, scheduledTime, 
                      priority, status, instructions);
        
        // Boutons
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.addClassNames("mt-m", "gap-s");
        
        Button cancelButton = new Button("Annuler", e -> close());
        cancelButton.addClassNames("button-secondary");
        
        Button saveButton = new Button("Enregistrer", e -> saveExam());
        saveButton.addClassNames("button-primary");
        
        buttonLayout.add(cancelButton, saveButton);
        
        // Layout principal
        VerticalLayout layout = new VerticalLayout(title, formLayout, buttonLayout);
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setMargin(false);
        
        add(layout);
    }
    
    private void configureFields() {
        // Configuration des champs
        patient.setPlaceholder("Sélectionner un patient");
        patient.setRequired(true);
        patient.setRequiredIndicatorVisible(true);
        patient.setItems(patientService.findAll());
        patient.setItemLabelGenerator(p -> p != null ? p.getFullName() : "");
        
        examType.setPlaceholder("Sélectionner le type d'examen");
        examType.setRequired(true);
        examType.setRequiredIndicatorVisible(true);
        examType.setItems(ExamType.values());
        examType.setItemLabelGenerator(type -> {
            switch (type) {
                case CT: return "Scanner (CT)";
                case MRI: return "IRM (MRI)";
                case RX: return "Radiographie (RX)";
                case ECHO: return "Échographie";
                case MAMMO: return "Mammographie";
                case FLUORO: return "Fluoroscopie";
                case PET: return "TEP (PET)";
                default: return type.toString();
            }
        });
        
        modality.setPlaceholder("Modalité automatique");
        modality.setReadOnly(true);
        modality.setHelperText("Calculée automatiquement selon le type d'examen");
        
        scheduledDate.setPlaceholder("JJ/MM/AAAA");
        scheduledDate.setRequired(true);
        scheduledDate.setRequiredIndicatorVisible(true);
        scheduledDate.setMin(LocalDate.now());
        
        scheduledTime.setPlaceholder("HH:MM");
        scheduledTime.setRequired(true);
        scheduledTime.setRequiredIndicatorVisible(true);
        
        priority.setPlaceholder("Sélectionner la priorité");
        priority.setRequired(true);
        priority.setRequiredIndicatorVisible(true);
        priority.setItems(Priority.values());
        priority.setValue(Priority.NORMAL);
        priority.setItemLabelGenerator(p -> {
            switch (p) {
                case URGENT: return "Urgent";
                case CRITICAL: return "Critique";
                case NORMAL: return "Normal";
                default: return p.toString();
            }
        });
        
        status.setPlaceholder("Sélectionner le statut");
        status.setRequired(true);
        status.setRequiredIndicatorVisible(true);
        status.setItems(ExamStatus.values());
        status.setValue(ExamStatus.PLANNED);
        status.setItemLabelGenerator(s -> {
            switch (s) {
                case PLANNED: return "Programmé";
                case IN_PROGRESS: return "En cours";
                case COMPLETED: return "Terminé";
                case CANCELLED: return "Annulé";
                default: return s.toString();
            }
        });
        
        instructions.setPlaceholder("Instructions pour l'examen...");
        instructions.setHeight("100px");
        instructions.setMaxLength(1000);
        instructions.setHelperText("Maximum 1000 caractères");
        
        // Mettre à jour la modalité quand le type d'examen change
        examType.addValueChangeListener(e -> updateModality(e.getValue()));
        
        // Synchroniser les changements de date et heure
        scheduledDate.addValueChangeListener(e -> {
            if (e.getValue() != null && scheduledTime.getValue() != null) {
                LocalDateTime newDateTime = e.getValue().atTime(scheduledTime.getValue());
                if (binder.getBean() != null) {
                    binder.getBean().setScheduledDateTime(newDateTime);
                }
            }
        });
        
        scheduledTime.addValueChangeListener(e -> {
            if (e.getValue() != null && scheduledDate.getValue() != null) {
                LocalDateTime newDateTime = scheduledDate.getValue().atTime(e.getValue());
                if (binder.getBean() != null) {
                    binder.getBean().setScheduledDateTime(newDateTime);
                }
            }
        });
    }
    
    private void configureBinder() {
        // Validation du patient
        binder.forField(patient)
                .asRequired("Le patient est requis")
                .bind(Exam::getPatient, Exam::setPatient);
        
        // Validation du type d'examen
        binder.forField(examType)
                .asRequired("Le type d'examen est requis")
                .bind(Exam::getExamType, Exam::setExamType);
        
        // Validation de la modalité
        binder.forField(modality)
                .asRequired("La modalité est requise")
                .bind(Exam::getModality, Exam::setModality);
        
        // Validation de la date et heure programmées
        binder.forField(scheduledDate)
                .asRequired("La date programmée est requise")
                .withValidator(date -> date != null && !date.isBefore(LocalDate.now()), 
                    "La date programmée ne peut pas être dans le passé")
                .bind(exam -> {
                    if (exam.getScheduledDateTime() == null) return null;
                    return exam.getScheduledDateTime().toLocalDate();
                }, (exam, date) -> {
                    LocalTime time = scheduledTime.getValue() != null ? scheduledTime.getValue() : LocalTime.now();
                    exam.setScheduledDateTime(date.atTime(time));
                });
        
        binder.forField(scheduledTime)
                .asRequired("L'heure programmée est requise")
                .bind(exam -> {
                    if (exam.getScheduledDateTime() == null) return null;
                    return exam.getScheduledDateTime().toLocalTime();
                }, (exam, time) -> {
                    LocalDate date = scheduledDate.getValue() != null ? scheduledDate.getValue() : LocalDate.now();
                    exam.setScheduledDateTime(date.atTime(time));
                });
        
        // Validation de la priorité
        binder.forField(priority)
                .asRequired("La priorité est requise")
                .bind(Exam::getPriority, Exam::setPriority);
        
        // Validation du statut
        binder.forField(status)
                .asRequired("Le statut est requis")
                .bind(Exam::getStatus, Exam::setStatus);
        
        // Validation des instructions
        binder.forField(instructions)
                .withValidator(new StringLengthValidator(
                    "Les instructions ne doivent pas dépasser 1000 caractères", 0, 1000))
                .bind(Exam::getAdditionalInstructions, Exam::setAdditionalInstructions);
    }
    
    private void bindFields() {
        // Les champs sont déjà liés dans configureBinder()
    }
    
    private void updateModality(ExamType examType) {
        if (examType != null) {
            switch (examType) {
                case CT:
                    modality.setValue("CT");
                    break;
                case MRI:
                    modality.setValue("MR");
                    break;
                case RX:
                    modality.setValue("XR");
                    break;
                case ECHO:
                    modality.setValue("US");
                    break;
                case MAMMO:
                    modality.setValue("MG");
                    break;
                case FLUORO:
                    modality.setValue("RF");
                    break;
                case PET:
                    modality.setValue("PT");
                    break;
                default:
                    modality.setValue(examType.name());
            }
        } else {
            modality.clear();
        }
    }
    
    private void saveExam() {
        if (binder.validate().isOk()) {
            Exam examToSave = binder.getBean();
            saveCallback.accept(examToSave);
            close();
        }
    }
    
    /**
     * Configure la validation du dialogue (appelée depuis l'extérieur si nécessaire)
     */
    public void configureValidation() {
        // La validation est déjà configurée dans configureBinder()
    }
}
