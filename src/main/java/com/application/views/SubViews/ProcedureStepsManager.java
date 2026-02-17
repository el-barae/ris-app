package com.application.views.SubViews;

import com.application.entity.Procedure;
import com.application.entity.ProcedureStep;
import com.application.repository.ProcedureStepRepository;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;

import java.util.List;

public class ProcedureStepsManager {

    private final ProcedureStepRepository procedureStepRepository;
    private Grid<ProcedureStep> stepsGrid;
    private Procedure currentProcedure;

    public ProcedureStepsManager(ProcedureStepRepository procedureStepRepository) {
        this.procedureStepRepository = procedureStepRepository;
    }

    public Component createProcedureStepsEditor(Procedure procedure, Dialog parentDialog) {
        this.currentProcedure = procedure;

        VerticalLayout layout = new VerticalLayout();
        layout.setWidthFull();
        layout.setSpacing(true);
        layout.setPadding(true);

        // Header
        H3 header = new H3("Étapes de la Procédure");
        header.getStyle()
                .set("margin", "0 0 1rem 0")
                .set("color", "#374151")
                .set("border-bottom", "2px solid #10b981")
                .set("padding-bottom", "0.5rem");

        // Grid pour les étapes
        configureStepsGrid();
        
        // Boutons d'action
        HorizontalLayout actionsLayout = new HorizontalLayout();
        actionsLayout.setSpacing(true);
        actionsLayout.setWidthFull();

        Button addStepBtn = new Button("Ajouter une étape", VaadinIcon.PLUS.create());
        addStepBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addStepBtn.addClickListener(e -> openStepEditorDialog(null));

        Button refreshBtn = new Button("Actualiser", VaadinIcon.REFRESH.create());
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        refreshBtn.addClickListener(e -> refreshGrid());

        actionsLayout.add(addStepBtn, refreshBtn);
        actionsLayout.setFlexGrow(1, addStepBtn);

        layout.add(header, actionsLayout, stepsGrid);
        
        // Charger les étapes existantes
        refreshGrid();

        return layout;
    }

    private void configureStepsGrid() {
        stepsGrid = new Grid<>(ProcedureStep.class);
        stepsGrid.setWidthFull();
        stepsGrid.setHeight("300px");
        stepsGrid.removeAllColumns();

        // Colonne Ordre
        stepsGrid.addColumn(ProcedureStep::getStepOrder)
                .setHeader("Ordre")
                .setWidth("80px")
                .setSortable(true);

        // Colonne Nom
        stepsGrid.addColumn(ProcedureStep::getName)
                .setHeader("Nom")
                .setFlexGrow(1);

        // Colonne Durée estimée
        stepsGrid.addColumn(step -> step.getEstimatedDurationMinutes() != null ? step.getEstimatedDurationMinutes() + " min" : "N/A")
                .setHeader("Durée est.")
                .setWidth("100px");

        // Colonne Requis
        stepsGrid.addColumn(step -> step.getIsRequired() != null && step.getIsRequired() ? "Oui" : "Non")
                .setHeader("Requis")
                .setWidth("80px");

        // Colonne Complété
        stepsGrid.addColumn(step -> step.getIsCompleted() != null && step.getIsCompleted() ? "✓" : "○")
                .setHeader("Terminé")
                .setWidth("80px");

        // Colonne Actions
        stepsGrid.addComponentColumn(this::createStepActions)
                .setHeader("Actions")
                .setWidth("120px");

        // Charger les étapes depuis la base de données pour éviter LazyInitializationException
        refreshGrid();
    }

    private Component createStepActions(ProcedureStep step) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(true);

        Button editBtn = new Button(VaadinIcon.EDIT.create());
        editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        editBtn.getElement().setProperty("title", "Modifier l'étape");
        editBtn.addClickListener(e -> openStepEditorDialog(step));

        Button deleteBtn = new Button(VaadinIcon.TRASH.create());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        deleteBtn.getElement().setProperty("title", "Supprimer l'étape");
        deleteBtn.addClickListener(e -> deleteStep(step));

        Button upBtn = new Button(VaadinIcon.ANGLE_UP.create());
        upBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        upBtn.getElement().setProperty("title", "Monter");
        upBtn.addClickListener(e -> moveStepUp(step));

        Button downBtn = new Button(VaadinIcon.ANGLE_DOWN.create());
        downBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        downBtn.getElement().setProperty("title", "Descendre");
        downBtn.addClickListener(e -> moveStepDown(step));

        actions.add(editBtn, deleteBtn, upBtn, downBtn);
        return actions;
    }

    private void openStepEditorDialog(ProcedureStep existingStep) {
        Dialog stepDialog = new Dialog();
        stepDialog.setHeaderTitle(existingStep == null ? "Nouvelle étape" : "Modifier l'étape");
        stepDialog.setWidth("500px");

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);

        // Binder pour la validation
        Binder<ProcedureStep> binder = new Binder<>(ProcedureStep.class);
        ProcedureStep step = existingStep != null ? existingStep : new ProcedureStep();
        final ProcedureStep finalStep = step;

        // Champ Nom
        TextField nameField = new TextField("Nom de l'étape");
        nameField.setWidthFull();
        nameField.setRequired(true);
        binder.forField(nameField)
                .asRequired("Le nom est requis")
                .bind(ProcedureStep::getName, ProcedureStep::setName);

        // Champ Description
        TextArea descriptionField = new TextArea("Description");
        descriptionField.setWidthFull();
        descriptionField.setHeight("100px");
        binder.bind(descriptionField, ProcedureStep::getDescription, ProcedureStep::setDescription);

        // Champ Ordre
        IntegerField orderField = new IntegerField("Ordre");
        orderField.setWidthFull();
        orderField.setMin(1);
        orderField.setValue(existingStep != null ? step.getStepOrder() : getNextOrder());
        binder.forField(orderField)
                .asRequired("L'ordre est requis")
                .withValidator(order -> order > 0, "L'ordre doit être supérieur à 0")
                .bind(ProcedureStep::getStepOrder, ProcedureStep::setStepOrder);

        // Champ Durée estimée
        IntegerField durationField = new IntegerField("Durée estimée (minutes)");
        durationField.setWidthFull();
        durationField.setMin(1);
        binder.forField(durationField)
                .withValidator(duration -> duration == null || duration > 0, "La durée doit être supérieure à 0")
                .bind(ProcedureStep::getEstimatedDurationMinutes, ProcedureStep::setEstimatedDurationMinutes);

        // Champ Instructions
        TextArea instructionsField = new TextArea("Instructions");
        instructionsField.setWidthFull();
        instructionsField.setHeight("80px");
        binder.bind(instructionsField, ProcedureStep::getInstructions, ProcedureStep::setInstructions);

        // Checkbox Requis
        com.vaadin.flow.component.checkbox.Checkbox requiredCheckbox = new com.vaadin.flow.component.checkbox.Checkbox("Étape requise");
        binder.bind(requiredCheckbox, ProcedureStep::getIsRequired, ProcedureStep::setIsRequired);

        content.add(nameField, descriptionField, orderField, durationField, instructionsField, requiredCheckbox);

        // Remplir les champs si modification
        if (existingStep != null) {
            binder.readBean(existingStep);
        }

        // Boutons
        Button saveBtn = new Button("Enregistrer", e -> {
            try {
                binder.writeBean(finalStep);
                
                if (existingStep == null) {
                    // Nouvelle étape
                    finalStep.setProcedure(currentProcedure);
                    procedureStepRepository.save(finalStep);
                    // Pas besoin d'appeler addProcedureStep - la relation est déjà établie par setProcedure
                } else {
                    // Modification
                    procedureStepRepository.save(finalStep);
                }
                
                refreshGrid();
                stepDialog.close();
                Notification.show("Étape enregistrée avec succès", 3000, Notification.Position.BOTTOM_END);
                
            } catch (ValidationException ex) {
                Notification.show("Veuillez corriger les erreurs", 3000, Notification.Position.MIDDLE);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Annuler", e -> stepDialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        HorizontalLayout buttons = new HorizontalLayout(saveBtn, cancelBtn);
        buttons.setSpacing(true);

        stepDialog.add(content);
        stepDialog.getFooter().add(buttons);
        stepDialog.open();
    }

    private void deleteStep(ProcedureStep step) {
        com.vaadin.flow.component.confirmdialog.ConfirmDialog dialog = new com.vaadin.flow.component.confirmdialog.ConfirmDialog();
        dialog.setHeader("Confirmation de suppression");
        dialog.setText("Voulez-vous vraiment supprimer cette étape ?");
        dialog.setCancelable(true);
        dialog.setConfirmText("Supprimer");
        dialog.setConfirmButtonTheme("error primary");
        
        dialog.addConfirmListener(e -> {
            // Pas besoin d'appeler removeProcedureStep - le repository delete gère la suppression
            procedureStepRepository.delete(step);
            refreshGrid();
            Notification.show("Étape supprimée avec succès", 3000, Notification.Position.BOTTOM_END);
        });
        
        dialog.open();
    }

    private void moveStepUp(ProcedureStep step) {
        // Charger les étapes depuis la base de données pour éviter LazyInitializationException
        List<ProcedureStep> steps = procedureStepRepository.findByProcedureIdOrderByStepOrder(currentProcedure.getId());
        
        int currentIndex = -1;
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).getId().equals(step.getId())) {
                currentIndex = i;
                break;
            }
        }
        
        if (currentIndex > 0) {
            ProcedureStep previousStep = steps.get(currentIndex - 1);
            
            // Échanger les ordres
            int tempOrder = step.getStepOrder();
            step.setStepOrder(previousStep.getStepOrder());
            previousStep.setStepOrder(tempOrder);
            
            procedureStepRepository.save(step);
            procedureStepRepository.save(previousStep);
            
            refreshGrid();
        }
    }

    private void moveStepDown(ProcedureStep step) {
        // Charger les étapes depuis la base de données pour éviter LazyInitializationException
        List<ProcedureStep> steps = procedureStepRepository.findByProcedureIdOrderByStepOrder(currentProcedure.getId());
        
        int currentIndex = -1;
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).getId().equals(step.getId())) {
                currentIndex = i;
                break;
            }
        }
        
        if (currentIndex >= 0 && currentIndex < steps.size() - 1) {
            ProcedureStep nextStep = steps.get(currentIndex + 1);
            
            // Échanger les ordres
            int tempOrder = step.getStepOrder();
            step.setStepOrder(nextStep.getStepOrder());
            nextStep.setStepOrder(tempOrder);
            
            procedureStepRepository.save(step);
            procedureStepRepository.save(nextStep);
            
            refreshGrid();
        }
    }

    private void refreshGrid() {
        if (currentProcedure != null && stepsGrid != null) {
            // Charger les étapes depuis la base de données pour éviter LazyInitializationException
            List<ProcedureStep> steps = procedureStepRepository.findByProcedureIdOrderByStepOrder(currentProcedure.getId());
            stepsGrid.setItems(steps);
        }
    }

    private Integer getNextOrder() {
        if (currentProcedure == null) {
            return 1;
        }
        // Charger les étapes depuis la base de données pour éviter LazyInitializationException
        List<ProcedureStep> steps = procedureStepRepository.findByProcedureIdOrderByStepOrder(currentProcedure.getId());
        if (steps.isEmpty()) {
            return 1;
        }
        return steps.get(steps.size() - 1).getStepOrder() + 1;
    }

    public Procedure getCurrentProcedure() {
        return currentProcedure;
    }

    public void setCurrentProcedure(Procedure procedure) {
        this.currentProcedure = procedure;
        refreshGrid();
    }
}
