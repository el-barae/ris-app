package com.application.views;

import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.UI;
import com.application.security.SecurityUtils;

@Route("")
@RouteAlias("/")
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
        // Debug: vérifier l'état de l'authentification
        boolean isLoggedIn = SecurityUtils.isUserLoggedIn();
        System.out.println("DEBUG: User logged in? " + isLoggedIn);
        
        // Vérifier si l'utilisateur est authentifié
        if (!isLoggedIn) {
            System.out.println("DEBUG: Redirecting to login...");
            // Rediriger vers la page de login avec JavaScript comme fallback
            UI.getCurrent().navigate("login");
            
            // Forcer la redirection avec JavaScript si la navigation Vaadin ne fonctionne pas
            UI.getCurrent().getPage().executeJs(
                "setTimeout(function() {" +
                "  console.log('DEBUG: JavaScript redirect to login');" +
                "  window.location.href = '/login';" +
                "}, 500);"
            );
            return;
        }
        
        System.out.println("DEBUG: User is logged in, redirecting to dashboard...");
        // Si authentifié, rediriger vers le dashboard
        UI.getCurrent().navigate("dashboard");
        
        // Forcer la redirection avec JavaScript comme fallback
        UI.getCurrent().getPage().executeJs(
            "setTimeout(function() {" +
            "  console.log('DEBUG: JavaScript redirect to dashboard');" +
            "  window.location.href = '/dashboard';" +
            "}, 500);"
        );
    }
}
