package com.application.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("RIS - Connexion")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();

    public LoginView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        setPadding(false);
        setMargin(false);
        
        // Style du fond
        addClassName("login-view");
        getStyle()
            .set("background", "linear-gradient(135deg, #f5f5f5 0%, #e5e7eb 100%)")
            .set("min-height", "100vh")
            .set("width", "100%")
            .set("display", "flex")
            .set("justify-content", "center")
            .set("align-items", "center")
            .set("margin", "0")
            .set("padding", "0");
        
        add(createLoginForm());
    }

    private Component createLoginForm() {
        Div loginContainer = new Div();
        loginContainer.addClassNames("login-form", "card", "p-l");
        loginContainer.setMaxWidth("450px");
        loginContainer.setWidth("100%");
        loginContainer.getStyle()
            .set("margin", "0 auto")
            .set("display", "block");
        
        // Style du conteneur
        loginContainer.getStyle()
            .set("background", "white")
            .set("border-radius", "16px")
            .set("box-shadow", "0 8px 32px rgba(0, 0, 0, 0.1)")
            .set("border", "1px solid #e5e7eb")
            .set("padding", "2rem")
            .set("backdrop-filter", "blur(10px)");

        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setSpacing(true);
        formLayout.setPadding(false);
        formLayout.setAlignItems(Alignment.CENTER);

        // Logo et titre
        Div titleContainer = new Div();
        titleContainer.getStyle()
            .set("text-align", "center")
            .set("margin-bottom", "1.5rem");
        
        Icon hospitalIcon = VaadinIcon.HOSPITAL.create();
        hospitalIcon.setSize("60px");
        hospitalIcon.getStyle()
            .set("color", "#10b981")
            .set("margin-bottom", "1rem")
            .set("display", "block")
            .set("margin-left", "auto")
            .set("margin-right", "auto");
        
        H1 title = new H1("RIS Sahty");
        title.getStyle()
            .set("color", "#10b981")
            .set("font-size", "2rem")
            .set("font-weight", "700")
            .set("margin", "0 0 0.5rem 0")
            .set("text-align", "center");

        H3 subtitle = new H3("Connexion");
        subtitle.getStyle()
            .set("color", "#6b7280")
            .set("font-size", "1.2rem")
            .set("font-weight", "500")
            .set("margin", "0 0 2rem 0")
            .set("text-align", "center");

        loginForm.setAction("login");
        loginForm.addClassNames("w-full");
        
        // Style du formulaire
        loginForm.getStyle()
            .set("--vaadin-input-field-border-width", "2px")
            .set("--vaadin-input-field-border-color", "#e5e7eb")
            .set("--vaadin-input-field-focus-border-color", "#10b981")
            .set("--vaadin-button-primary-background", "#10b981")
            .set("--vaadin-button-primary-focus-background", "#059669")
            .set("--vaadin-button-primary-border-color", "#10b981")
            .set("--vaadin-button-primary-text-color", "white");

        // Comptes de test
        Div testAccountsContainer = new Div();
        testAccountsContainer.getStyle()
            .set("background", "#f5f5f5")
            .set("border-radius", "8px")
            .set("padding", "1rem")
            .set("margin-top", "1.5rem")
            .set("border", "1px solid #e5e7eb");
        
        Paragraph testAccountsTitle = new Paragraph("Comptes de test :");
        testAccountsTitle.getStyle()
            .set("color", "#374151")
            .set("font-weight", "600")
            .set("margin", "0 0 0.5rem 0")
            .set("font-size", "0.9rem");
        
        Paragraph testAccounts = new Paragraph("admin/admin123 | medecin/medecin123 | radiologue/radio123");
        testAccounts.getStyle()
            .set("color", "#6b7280")
            .set("font-size", "0.85rem")
            .set("margin", "0")
            .set("line-height", "1.4");
        
        testAccountsContainer.add(testAccountsTitle, testAccounts);

        formLayout.add(titleContainer, hospitalIcon, title, subtitle, loginForm, testAccountsContainer);

        loginContainer.add(formLayout);
        return loginContainer;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean error = event.getLocation().getQueryParameters().getParameters().containsKey("error");
        loginForm.setError(error);
    }
}
