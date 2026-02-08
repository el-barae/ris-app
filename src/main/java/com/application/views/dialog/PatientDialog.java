package com.application.views.dialog;

import com.application.entity.Gender;
import com.application.entity.Patient;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.StringLengthValidator;

import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * Dialogue pour créer ou modifier un patient
 */
public class PatientDialog extends Dialog {
    
    private final Patient patient;
    private final Consumer<Patient> saveCallback;
    private final Binder<Patient> binder;
    private final boolean isEditMode;
    
    // Champs du formulaire
    private final TextField patientId = new TextField("ID Patient");
    private final TextField firstName = new TextField("Prénom");
    private final TextField lastName = new TextField("Nom");
    private final DatePicker dateOfBirth = new DatePicker("Date de naissance");
    private final ComboBox<Gender> gender = new ComboBox<>("Genre");
    private final TextField phone = new TextField("Téléphone");
    private final EmailField email = new EmailField("Email");
    private final TextField address = new TextField("Adresse");
    private final TextField city = new TextField("Ville");
    private final TextField postalCode = new TextField("Code postal");
    private final TextField cin = new TextField("CIN");
    private final TextField passportNumber = new TextField("Passeport");
    private final TextField nationality = new TextField("Nationalité");
    
    // Champs parentaux
    private final TextField parentFirstName = new TextField("Prénom du parent");
    private final TextField parentLastName = new TextField("Nom du parent");
    private final TextField parentPhone = new TextField("Téléphone du parent");
    private final TextField parentRelationship = new TextField("Relation");
    
    public PatientDialog(Patient patient, Consumer<Patient> saveCallback) {
        this.isEditMode = patient != null;
        this.patient = isEditMode ? patient : new Patient();
        this.saveCallback = saveCallback;
        this.binder = new Binder<>(Patient.class);
        
        configureDialog(isEditMode);
        configureFields();
        configureBinder();
        
        // Set the bean immediately after configuration
        binder.setBean(this.patient);
        
        bindFields();
    }
    
    private void configureDialog(boolean isEditMode) {
        setModal(true);
        setDraggable(true);
        setResizable(true);
        setWidth("600px");
        
        // Header
        H3 title = new H3(isEditMode ? "Modifier le patient" : "Nouveau patient");
        title.addClassNames("mb-m", "text-primary");
        
        // Formulaire principal
        FormLayout mainFormLayout = new FormLayout();
        mainFormLayout.setWidthFull();
        mainFormLayout.add(patientId, firstName, lastName, dateOfBirth, gender, 
                              phone, email, address, city, postalCode);
        
        // Section identité
        H4 identityTitle = new H4("Identité");
        identityTitle.addClassNames("mt-l", "mb-s", "text-secondary");
        
        FormLayout identityFormLayout = new FormLayout();
        identityFormLayout.setWidthFull();
        identityFormLayout.add(cin, passportNumber, nationality);
        
        // Section parentale
        H4 parentTitle = new H4("Informations parentales");
        parentTitle.addClassNames("mt-l", "mb-s", "text-secondary");
        
        FormLayout parentFormLayout = new FormLayout();
        parentFormLayout.setWidthFull();
        parentFormLayout.add(parentFirstName, parentLastName, parentPhone, parentRelationship);
        
        // Boutons
        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.addClassNames("mt-m", "gap-s");
        
        Button cancelButton = new Button("Annuler", e -> close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        
        Button saveButton = new Button("Enregistrer", e -> savePatient());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        
        buttonLayout.add(cancelButton, saveButton);
        
        // Layout principal
        VerticalLayout layout = new VerticalLayout(title, mainFormLayout, identityTitle, identityFormLayout, 
                                                   parentTitle, parentFormLayout, buttonLayout);
        layout.setPadding(true);
        layout.setSpacing(true);
        layout.setMargin(false);
        
        add(layout);
    }
    
    private void configureFields() {
        // Configuration des champs
        patientId.setPlaceholder("Ex: PAT001 (laisser vide pour génération automatique)");
        patientId.setRequired(false);
        patientId.setRequiredIndicatorVisible(false);
        
        firstName.setPlaceholder("Ex: Jean");
        firstName.setRequired(true);
        firstName.setRequiredIndicatorVisible(true);
        
        lastName.setPlaceholder("Ex: Dupont");
        lastName.setRequired(true);
        lastName.setRequiredIndicatorVisible(true);
        
        dateOfBirth.setPlaceholder("JJ/MM/AAAA");
        dateOfBirth.setRequired(true);
        dateOfBirth.setRequiredIndicatorVisible(true);
        dateOfBirth.setMax(LocalDate.now());
        
        gender.setItems(Gender.values());
        gender.setRequired(true);
        gender.setRequiredIndicatorVisible(true);
        gender.setItemLabelGenerator(gender -> {
            switch (gender) {
                case MALE: return "Homme";
                case FEMALE: return "Femme";
                default: return gender.toString();
            }
        });
        
        phone.setPlaceholder("Ex: 06 12 34 56 78");
        
        email.setPlaceholder("Ex: jean.dupont@email.com");
        email.setClearButtonVisible(true);
        
        address.setPlaceholder("Ex: 123 rue de la République");
        
        city.setPlaceholder("Ex: Paris");
        
        postalCode.setPlaceholder("Ex: 75001");
        
        cin.setPlaceholder("Ex: AB123456");
        cin.setRequiredIndicatorVisible(false);
        
        passportNumber.setPlaceholder("Ex: 12AB34567");
        passportNumber.setRequiredIndicatorVisible(false);
        
        nationality.setPlaceholder("Ex: Française");
        nationality.setRequiredIndicatorVisible(false);
        
        // Configuration des champs parentaux
        parentFirstName.setPlaceholder("Ex: Marie");
        parentFirstName.setRequiredIndicatorVisible(false);
        
        parentLastName.setPlaceholder("Ex: Dupont");
        parentLastName.setRequiredIndicatorVisible(false);
        
        parentPhone.setPlaceholder("Ex: 06 12 34 56 78");
        parentPhone.setRequiredIndicatorVisible(false);
        
        parentRelationship.setPlaceholder("Ex: Mère, Père, Tuteur");
        parentRelationship.setRequiredIndicatorVisible(false);
    }
    
    private void configureBinder() {
        // Validation du Patient ID
        binder.forField(patientId)
                .withValidator(new StringLengthValidator(
                    "L'ID patient doit contenir entre 3 et 20 caractères", 3, 20))
                .withValidator(id -> id == null || id.trim().isEmpty() || id.matches("^[A-Z0-9_]+$"), 
                    "L'ID patient ne peut contenir que des lettres majuscules, chiffres et underscores")
                .bind(Patient::getPatientId, Patient::setPatientId);
        
        // Validation du prénom
        binder.forField(firstName)
                .withValidator(new StringLengthValidator(
                    "Le prénom doit contenir entre 2 et 50 caractères", 2, 50))
                .asRequired("Le prénom est requis")
                .bind(Patient::getFirstName, Patient::setFirstName);
        
        // Validation du nom
        binder.forField(lastName)
                .withValidator(new StringLengthValidator(
                    "Le nom doit contenir entre 2 et 50 caractères", 2, 50))
                .asRequired("Le nom est requis")
                .bind(Patient::getLastName, Patient::setLastName);
        
        // Validation de la date de naissance
        binder.forField(dateOfBirth)
                .asRequired("La date de naissance est requise")
                .withValidator(date -> date != null && date.isBefore(LocalDate.now()), 
                    "La date de naissance doit être antérieure à aujourd'hui")
                .bind(Patient::getDateOfBirth, Patient::setDateOfBirth);
        
        // Validation du genre
        binder.forField(gender)
                .asRequired("Le genre est requis")
                .bind(Patient::getGender, Patient::setGender);
        
        // Validation du téléphone
        binder.forField(phone)
                .withValidator(phone -> phone == null || phone.trim().isEmpty() || 
                    phone.matches("^[0-9\\s\\-\\.\\(\\)]+$"), 
                    "Le numéro de téléphone n'est pas valide")
                .bind(Patient::getPhone, Patient::setPhone);
        
        // Validation de l'email
        binder.forField(email)
                .withValidator(email -> email == null || email.trim().isEmpty() || 
                    email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"), 
                    "L'email n'est pas valide")
                .bind(Patient::getEmail, Patient::setEmail);
        
        // Validation de l'adresse
        binder.forField(address)
                .withValidator(new StringLengthValidator(
                    "L'adresse ne doit pas dépasser 200 caractères", 0, 200))
                .bind(Patient::getAddress, Patient::setAddress);
        
        // Validation de la ville
        binder.forField(city)
                .withValidator(new StringLengthValidator(
                    "La ville ne doit pas dépasser 100 caractères", 0, 100))
                .bind(Patient::getCity, Patient::setCity);
        
        // Validation du code postal
        binder.forField(postalCode)
                .withValidator(postal -> postal == null || postal.trim().isEmpty() || 
                    postal.matches("^[0-9]{5}$"), 
                    "Le code postal doit contenir 5 chiffres")
                .bind(Patient::getPostalCode, Patient::setPostalCode);
        
        // Validation du CIN
        binder.forField(cin)
                .withValidator(new StringLengthValidator(
                    "Le CIN ne doit pas dépasser 20 caractères", 0, 20))
                .withValidator(cinValue -> cinValue == null || cinValue.trim().isEmpty() || 
                    cinValue.matches("^[A-Za-z0-9]+$"), 
                    "Le CIN ne peut contenir que des lettres et des chiffres")
                .bind(Patient::getCin, Patient::setCin);
        
        // Validation du passeport
        binder.forField(passportNumber)
                .withValidator(new StringLengthValidator(
                    "Le passeport ne doit pas dépasser 50 caractères", 0, 50))
                .withValidator(passport -> passport == null || passport.trim().isEmpty() || 
                    passport.matches("^[A-Za-z0-9]+$"), 
                    "Le passeport ne peut contenir que des lettres et des chiffres")
                .bind(Patient::getPassportNumber, Patient::setPassportNumber);
        
        // Validation de la nationalité
        binder.forField(nationality)
                .withValidator(new StringLengthValidator(
                    "La nationalité ne doit pas dépasser 100 caractères", 0, 100))
                .bind(Patient::getNationality, Patient::setNationality);
        
        // Validation du prénom du parent
        binder.forField(parentFirstName)
                .withValidator(new StringLengthValidator(
                    "Le prénom du parent ne doit pas dépasser 50 caractères", 0, 50))
                .bind(Patient::getParentFirstName, Patient::setParentFirstName);
        
        // Validation du nom du parent
        binder.forField(parentLastName)
                .withValidator(new StringLengthValidator(
                    "Le nom du parent ne doit pas dépasser 50 caractères", 0, 50))
                .bind(Patient::getParentLastName, Patient::setParentLastName);
        
        // Validation du téléphone du parent
        binder.forField(parentPhone)
                .withValidator(phone -> phone == null || phone.trim().isEmpty() || 
                    phone.matches("^[0-9\\s\\-\\.\\(\\)]+$"), 
                    "Le numéro de téléphone du parent n'est pas valide")
                .bind(Patient::getParentPhone, Patient::setParentPhone);
        
        // Validation de la relation parentale
        binder.forField(parentRelationship)
                .withValidator(new StringLengthValidator(
                    "La relation ne doit pas dépasser 50 caractères", 0, 50))
                .bind(Patient::getParentRelationship, Patient::setParentRelationship);
    }
    
    private void bindFields() {
        // Les champs sont déjà liés dans configureBinder()
    }
    
    private void savePatient() {
        if (binder.validate().isOk()) {
            Patient patientToSave = binder.getBean();
            
            // Generate patient ID if empty for new patients
            if (!isEditMode && (patientToSave.getPatientId() == null || patientToSave.getPatientId().trim().isEmpty())) {
                patientToSave.setPatientId(generatePatientId());
            }
            
            saveCallback.accept(patientToSave);
            close();
        }
    }
    
    private String generatePatientId() {
        return "P" + String.format("%08d", (int)(Math.random() * 100000000));
    }
    
    /**
     * Configure la validation du dialogue (appelée depuis l'extérieur si nécessaire)
     */
    public void configureValidation() {
        // La validation est déjà configurée dans configureBinder()
    }
}
