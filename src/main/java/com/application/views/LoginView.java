package com.application.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
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
        
        // Ajouter une marge en haut
        addClassName("login-view");
        getStyle().set("margin-top", "1rem");
        
        add(createLoginForm());
    }

    private Component createLoginForm() {
        Div loginContainer = new Div();
        loginContainer.addClassNames("login-form", "card", "p-l");
        loginContainer.setMaxWidth("400px");
        loginContainer.setWidthFull();

        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setSpacing(true);
        formLayout.setPadding(false);
        formLayout.setAlignItems(Alignment.STRETCH);

        // Titre
        H1 title = new H1();
        title.add(VaadinIcon.HOSPITAL.create());
        title.add(" Système RIS");
        title.addClassNames("text-center", "mb-m");

        H3 subtitle = new H3("Radiologie - Connexion");
        subtitle.addClassNames("text-center", "mb-l");

        loginForm.setAction("login");
        loginForm.addClassNames("w-full");

        // Comptes de test
        Paragraph testAccounts = new Paragraph();
        testAccounts.getElement().setProperty("innerHTML", 
            "Comptes de test :<br>admin/admin123 | medecin/medecin123 | radiologue/radio123");
        testAccounts.addClassNames("text-secondary", "text-s", "text-center", "mt-m");

        formLayout.add(title, subtitle, loginForm, testAccounts);

        loginContainer.add(formLayout);
        return loginContainer;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        boolean error = event.getLocation().getQueryParameters().getParameters().containsKey("error");
        loginForm.setError(error);
    }
}
