package com.application.views;

import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.server.VaadinServletRequest;
import jakarta.servlet.http.HttpServletRequest;

@Route("")
@PageTitle("RIS Radiologie")
@AnonymousAllowed
public class MainView extends VerticalLayout implements BeforeEnterObserver {

    public MainView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        
        // Ajouter une marge en haut
        addClassName("main-view");
        getStyle().set("margin-top", "5rem");
        
        // Message de chargement
        Div loadingContainer = new Div();
        loadingContainer.addClassName("loading-container");
        loadingContainer.getStyle()
                .set("text-align", "center")
                .set("padding", "2rem")
                .set("background", "white")
                .set("border-radius", "12px")
                .set("box-shadow", "0 4px 6px rgba(0,0,0,0.1)");

        H2 title = new H2();
        title.add(VaadinIcon.HOSPITAL.create());
        title.add(" RIS Sahty");
        title.getStyle()
                .set("color", "#10b981")
                .set("margin-bottom", "1rem");

        Paragraph message = new Paragraph("Redirection en cours...");
        message.getStyle()
                .set("color", "#6b7280")
                .set("font-size", "16px");

        loadingContainer.add(title, message);
        add(loadingContainer);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Vérifier si l'utilisateur est authentifié
        HttpServletRequest request = VaadinServletRequest.getCurrent().getHttpServletRequest();
        boolean isAuthenticated = request.getUserPrincipal() != null;
        
        if (isAuthenticated) {
            // Rediriger vers le dashboard si authentifié
            event.forwardTo("dashboard");
        } else {
            // Rediriger vers la page de login si non authentifié
            event.forwardTo("login");
        }
    }
}
