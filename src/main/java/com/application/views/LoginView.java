package com.application.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.component.UI;
import com.application.security.VaadinAuthService;
import com.application.security.SecurityUtils;

@Route("login")
@PageTitle("RIS - Connexion")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();
    protected final VaadinAuthService authService;

    public LoginView(VaadinAuthService authService) {
        this.authService = authService;
        
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
        
        // Configure login form listener
        loginForm.addLoginListener(event -> {
            boolean success = authService.authenticate(event.getUsername(), event.getPassword());
            if (success) {
                UI.getCurrent().navigate("dashboard");
            } else {
                loginForm.setError(true);
            }
        });
        
        // Ajouter le formulaire immédiatement
        add(createLoginForm());
        
        // Forcer l'affichage immédiat du formulaire avec plusieurs approches
        UI.getCurrent().getPage().executeJs(
            "// Forcer l'affichage du formulaire de login\n" +
            "function forceShowLoginForm() {\n" +
            "  console.log('Tentative d\\'affichage du formulaire de login');\n" +
            "  \n" +
            "  // Attendre que le DOM soit prêt\n" +
            "  const checkAndShow = () => {\n" +
            "    const loginForm = document.querySelector('vaadin-login-form');\n" +
            "    const loginOverlay = document.querySelector('vaadin-login-overlay-wrapper');\n" +
            "    \n" +
            "    if (loginForm) {\n" +
            "      console.log('Formulaire trouvé, forçage affichage');\n" +
            "      loginForm.style.display = 'block';\n" +
            "      loginForm.style.visibility = 'visible';\n" +
            "      loginForm.style.opacity = '1';\n" +
            "      \n" +
            "      // Forcer le rafraîchissement du composant\n" +
            "      if (loginForm.requestUpdate) {\n" +
            "        loginForm.requestUpdate();\n" +
            "      }\n" +
            "    }\n" +
            "    \n" +
            "    if (loginOverlay) {\n" +
            "      loginOverlay.style.display = 'block';\n" +
            "      loginOverlay.style.visibility = 'visible';\n" +
            "    }\n" +
            "    \n" +
            "    // Forcer le rafraîchissement de la page si le formulaire n'est pas visible\n" +
            "    const container = document.querySelector('.login-form');\n" +
            "    if (container && container.offsetParent === null) {\n" +
            "      console.log('Conteneur caché, forçage affichage');\n" +
            "      container.style.display = 'block';\n" +
            "      container.style.visibility = 'visible';\n" +
            "      container.style.position = 'relative';\n" +
            "    }\n" +
            "  };\n" +
            "  \n" +
            "  // Vérifier immédiatement puis toutes les 100ms pendant 2 secondes\n" +
            "  checkAndShow();\n" +
            "  let attempts = 0;\n" +
            "  const interval = setInterval(() => {\n" +
            "    checkAndShow();\n" +
            "    attempts++;\n" +
            "    if (attempts > 20) clearInterval(interval);\n" +
            "  }, 100);\n" +
            "}\n" +
            "\n" +
            "// Exécuter immédiatement et au chargement du DOM\n" +
            "forceShowLoginForm();\n" +
            "if (document.readyState === 'loading') {\n" +
            "  document.addEventListener('DOMContentLoaded', forceShowLoginForm);\n" +
            "} else {\n" +
            "  setTimeout(forceShowLoginForm, 50);\n" +
            "}"
        );
    }

    private Component createLoginForm() {
        Div loginContainer = new Div();
        loginContainer.addClassNames("login-form", "card", "p-l");
        loginContainer.setMaxWidth("450px");
        loginContainer.setWidth("100%");
        loginContainer.getStyle()
            .set("margin", "0 auto")
            .set("display", "flex")
            .set("justify-content", "center")
            .set("align-items", "center");
        
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
        formLayout.setWidth("100%");

        // Logo et titre
        Div titleContainer = new Div();
        titleContainer.getStyle()
            .set("text-align", "center")
            .set("margin-bottom", "1.5rem")
            .set("display", "flex")
            .set("flex-direction", "column")
            .set("align-items", "center")
            .set("gap", "1rem")
            .set("width", "100%");
        
        Image logoImage = new Image("images/sahty.jpeg", "RIS Sahty Logo");
        logoImage.setHeight("70px");
        logoImage.setWidth("70px");
        logoImage.getStyle()
            .set("border-radius", "12px")
            .set("box-shadow", "0 4px 16px rgba(16, 185, 129, 0.2)")
            .set("flex-shrink", "0")
            .set("margin", "8rem 0 0 0")
            .set("align-self", "center");
        
        H1 title = new H1("RIS Sahty");
        title.getStyle()
            .set("color", "#10b981")
            .set("font-size", "2rem")
            .set("font-weight", "700")
            .set("margin", "0")
            .set("text-align", "center")
            .set("align-self", "center");

        H3 subtitle = new H3("Connexion");
        subtitle.getStyle()
            .set("color", "#6b7280")
            .set("font-size", "1.2rem")
            .set("font-weight", "500")
            .set("margin", "0 0 1rem 0")
            .set("text-align", "center");

        loginForm.addClassNames("w-full");
        
        // Style du formulaire pour centrage horizontal
        loginForm.getStyle()
            .set("--vaadin-input-field-border-width", "2px")
            .set("--vaadin-input-field-border-color", "#e5e7eb")
            .set("--vaadin-input-field-focus-border-color", "#10b981")
            .set("--vaadin-button-primary-background", "#10b981")
            .set("--vaadin-button-primary-focus-background", "#059669")
            .set("--vaadin-button-primary-border-color", "#10b981")
            .set("--vaadin-button-primary-text-color", "white")
            .set("margin", "0 auto")
            .set("width", "100%")
            .set("max-width", "400px");

        // Comptes de test
        Div testAccountsContainer = new Div();
        testAccountsContainer.getStyle()
            .set("background", "#f5f5f5")
            .set("border-radius", "8px")
            .set("padding", "1rem")
            .set("margin-top", "1.5rem")
            .set("border", "1px solid #e5e7eb")
            .set("width", "100%")
            .set("text-align", "center");
        
        Paragraph testAccountsTitle = new Paragraph("Comptes de test :");
        testAccountsTitle.getStyle()
            .set("color", "#374151")
            .set("font-weight", "600")
            .set("margin", "0 0 0.5rem 0")
            .set("font-size", "0.9rem")
            .set("text-align", "center");
        
        Paragraph testAccounts = new Paragraph("admin/admin123 | medecin/medecin123 | radiologue/radio123");
        testAccounts.getStyle()
            .set("color", "#6b7280")
            .set("font-size", "0.85rem")
            .set("margin", "0")
            .set("line-height", "1.4")
            .set("text-align", "center");
        
        testAccountsContainer.add(testAccountsTitle, testAccounts);

        formLayout.add(titleContainer, logoImage, title, subtitle, loginForm, testAccountsContainer);

        loginContainer.add(formLayout);
        return loginContainer;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Si l'utilisateur est déjà authentifié, rediriger vers le dashboard
        if (SecurityUtils.isUserLoggedIn()) {
            UI.getCurrent().navigate("dashboard");
            return;
        }
        
        boolean error = event.getLocation().getQueryParameters().getParameters().containsKey("error");
        loginForm.setError(error);
    }
}
