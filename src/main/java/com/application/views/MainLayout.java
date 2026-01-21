package com.application.views;

import com.application.security.SecurityUtils;
import com.application.entity.UserRole;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import jakarta.annotation.security.PermitAll;
import com.vaadin.flow.spring.annotation.UIScope;
import org.springframework.stereotype.Component;
import com.vaadin.flow.component.UI;

@Component
@UIScope
@PermitAll
public class MainLayout extends AppLayout implements BeforeEnterObserver {

    public MainLayout() {
        setPrimarySection(Section.DRAWER);
        addToNavbar(createHeader());
        addToDrawer(createDrawer());
        setupWebSocketListener();
    }

    private void setupWebSocketListener() {
        UI.getCurrent().getPage().executeJs(
                "function loadScript(src) {" +
                        "  return new Promise((resolve, reject) => {" +
                        "    if (document.querySelector('script[src=\"' + src + '\"]')) {" +
                        "      resolve();" +
                        "      return;" +
                        "    }" +
                        "    const script = document.createElement('script');" +
                        "    script.src = src;" +
                        "    script.onload = () => {" +
                        "      console.log('✅ Chargé:', src);" +
                        "      resolve();" +
                        "    };" +
                        "    script.onerror = (err) => {" +
                        "      console.error('❌ Erreur chargement:', src, err);" +
                        "      reject(err);" +
                        "    };" +
                        "    document.head.appendChild(script);" +
                        "  });" +
                        "}" +
                        "" +
                        "Promise.all([" +
                        "  loadScript('https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js')," +
                        "  loadScript('https://cdn.jsdelivr.net/npm/@stomp/stompjs@5/bundles/stomp.umd.min.js')" +
                        "]).then(() => {" +
                        "  console.log('📚 Librairies WebSocket chargées');" +
                        "  const socket = new SockJS('/ws-exam-status');" +
                        "  const stompClient = Stomp.over(socket);" +
                        "  " +
                        "  stompClient.connect({}, function(frame) {" +
                        "    console.log('✅ WebSocket connecté:', frame);" +
                        "    " +
                        "    stompClient.subscribe('/topic/exam-status', function(message) {" +
                        "      const data = JSON.parse(message.body);" +
                        "      console.log('📨 Message MPPS reçu:', data);" +
                        "      " +
                        "      $0.$server.showStatusDialog(" +
                        "        data.accessionNumber," +
                        "        data.patientName," +
                        "        data.examType," +
                        "        data.newStatus," +
                        "        data.message" +
                        "      );" +
                        "    });" +
                        "  }, function(error) {" +
                        "    console.error('❌ Erreur connexion WebSocket:', error);" +
                        "  });" +
                        "  " +
                        "  window.addEventListener('beforeunload', function() {" +
                        "    if (stompClient && stompClient.connected) {" +
                        "      console.log('🔌 Déconnexion WebSocket');" +
                        "      stompClient.disconnect();" +
                        "    }" +
                        "  });" +
                        "}).catch(error => {" +
                        "  console.error('❌ Erreur chargement librairies WebSocket:', error);" +
                        "});",
                getElement()
        );
    }

    public void showStatusDialog(String accessionNumber, String patientName,
                                 String examType, String newStatus, String message) {
        UI.getCurrent().access(() -> {
            Dialog dialog = new Dialog();
            dialog.setCloseOnEsc(false);
            dialog.setCloseOnOutsideClick(false);
            dialog.setWidth("500px");

            // Titre avec icône
            HorizontalLayout header = new HorizontalLayout();
            Icon icon = getStatusIcon(newStatus);
            icon.setSize("32px");

            H3 title = new H3("Mise à jour d'examen");
            title.getStyle().set("margin", "0");
            header.add(icon, title);
            header.setAlignItems(FlexComponent.Alignment.CENTER);
            header.setSpacing(true);

            // Informations
            VerticalLayout content = new VerticalLayout();
            content.setPadding(true);
            content.setSpacing(true);

            Span messageSpan = new Span(message);
            messageSpan.getStyle()
                    .set("font-size", "16px")
                    .set("font-weight", "500")
                    .set("color", getStatusColor(newStatus));

            Span accessionSpan = new Span("Numéro d'accession: " + accessionNumber);
            Span patientSpan = new Span("Patient: " + patientName);
            Span examSpan = new Span("Type d'examen: " + examType);
            Span statusSpan = new Span("Nouveau statut: " + formatStatus(newStatus));
            statusSpan.getStyle()
                    .set("font-weight", "bold")
                    .set("color", getStatusColor(newStatus));

            content.add(messageSpan, accessionSpan, patientSpan, examSpan, statusSpan);

            // Boutons
            HorizontalLayout footer = new HorizontalLayout();
            footer.setWidthFull();
            footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
            footer.setSpacing(true);

            Button refreshButton = new Button("Actualiser la page", VaadinIcon.REFRESH.create());
            refreshButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            refreshButton.addClickListener(e -> {
                dialog.close();
                getUI().ifPresent(ui -> ui.getPage().reload());
            });

            Button closeButton = new Button("Fermer");
            closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            closeButton.addClickListener(e -> dialog.close());

            footer.add(closeButton, refreshButton);

            // Assemblage
            dialog.add(header, content, footer);
            dialog.open();

            // Notification sonore
            showNotification("Mise à jour d'examen: " + patientName, newStatus);
        });
    }

    private Icon getStatusIcon(String status) {
        Icon icon;
        if ("IN_PROGRESS".equals(status)) {
            icon = VaadinIcon.PLAY_CIRCLE.create();
            icon.setColor("#3b82f6");
        } else if ("COMPLETED".equals(status)) {
            icon = VaadinIcon.CHECK_CIRCLE.create();
            icon.setColor("#10b981");
        } else {
            icon = VaadinIcon.CLOSE_CIRCLE.create();
            icon.setColor("#ef4444");
        }
        return icon;
    }

    private String getStatusColor(String status) {
        switch (status) {
            case "IN_PROGRESS": return "#3b82f6";
            case "COMPLETED": return "#10b981";
            case "CANCELLED": return "#ef4444";
            default: return "#6b7280";
        }
    }

    private String formatStatus(String status) {
        switch (status) {
            case "IN_PROGRESS": return "En cours";
            case "COMPLETED": return "Terminé";
            case "CANCELLED": return "Annulé";
            default: return status;
        }
    }

    private void showNotification(String message, String status) {
        Notification notification = new Notification(message, 5000, Notification.Position.TOP_END);

        if ("COMPLETED".equals(status)) {
            notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } else if ("IN_PROGRESS".equals(status)) {
            notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
        } else {
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }

        notification.open();
    }

    private HorizontalLayout createHeader() {
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.addClassNames("main-header", "px-m", "py-xs");
        header.getStyle()
                .set("background", "linear-gradient(135deg, #667eea 0%, #764ba2 100%)")
                .set("color", "white")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)")
                .set("min-height", "60px");

        Button menuToggle = new Button(VaadinIcon.MENU.create());
        menuToggle.addClickListener(e -> setDrawerOpened(!isDrawerOpened()));
        menuToggle.addClassNames("menu-toggle");
        menuToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        menuToggle.getStyle()
                .set("color", "white")
                .set("background", "rgba(255,255,255,0.1)");
        header.add(menuToggle);

        H2 title = new H2();
        Icon hospitalIcon = VaadinIcon.HOSPITAL.create();
        hospitalIcon.getStyle().set("color", "white");
        title.add(hospitalIcon);
        title.add(" RIS Radiologie");
        title.addClassNames("text-truncate", "m-0");
        title.getStyle()
                .set("color", "white")
                .set("font-size", "1.5rem")
                .set("font-weight", "600");

        header.add(title);
        header.setFlexGrow(1, title);

        HorizontalLayout userSection = new HorizontalLayout();
        userSection.setAlignItems(FlexComponent.Alignment.CENTER);
        userSection.addClassNames("user-section", "gap-s");

        Avatar avatar = new Avatar();
        avatar.setName(getCurrentUserDisplayName());
        avatar.addClassNames("text-xs");
        avatar.getStyle()
                .set("background-color", "rgba(255,255,255,0.2)")
                .set("color", "white");

        Span userName = new Span(getCurrentUserDisplayName());
        userName.addClassNames("font-semibold", "text-s", "user-name");
        userName.getStyle()
                .set("color", "white")
                .set("font-weight", "500");

        Button logoutButton = new Button("Déconnexion", VaadinIcon.SIGN_OUT.create());
        logoutButton.addClassNames("logout-button");
        logoutButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        logoutButton.getStyle()
                .set("background", "rgba(255,255,255,0.2)")
                .set("color", "white")
                .set("border", "1px solid rgba(255,255,255,0.3)");
        logoutButton.addClickListener(e -> {
            SecurityUtils.logout();
            getUI().ifPresent(ui -> ui.navigate("login"));
        });

        userSection.add(avatar, userName, logoutButton);
        header.add(userSection);

        return header;
    }

    private VerticalLayout createDrawer() {
        VerticalLayout drawerLayout = new VerticalLayout();
        drawerLayout.setSizeFull();
        drawerLayout.setPadding(false);
        drawerLayout.setSpacing(false);
        drawerLayout.getStyle()
                .set("background-color", "#f8fafc")
                .set("padding-top", "1rem");

        HorizontalLayout drawerHeader = new HorizontalLayout();
        drawerHeader.setWidthFull();
        drawerHeader.setPadding(true);
        drawerHeader.setSpacing(true);
        drawerHeader.getStyle()
                .set("border-bottom", "2px solid #e2e8f0")
                .set("margin-bottom", "1rem");

        Icon menuIcon = VaadinIcon.MENU.create();
        menuIcon.setSize("24px");
        menuIcon.getStyle().set("color", "#667eea");

        H3 drawerTitle = new H3("Menu");
        drawerTitle.getStyle()
                .set("margin", "0")
                .set("color", "#1e293b")
                .set("font-size", "1.2rem");

        drawerHeader.add(menuIcon, drawerTitle);

        Tabs tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.addClassNames("flex-grow", "gap-s", "px-s");
        tabs.getStyle()
                .set("width", "100%")
                .set("background-color", "transparent");

        tabs.add(createTab("Dashboard", VaadinIcon.DASHBOARD, "dashboard"));
        tabs.add(createTab("Patients", VaadinIcon.USERS, "patients"));
        tabs.add(createTab("Examens", VaadinIcon.CLIPBOARD_TEXT, "exams"));
        tabs.add(createTab("WorkList", VaadinIcon.LIST, "worklist-dragdrop"));
        tabs.add(createTab("Rapports", VaadinIcon.FILE_TEXT, "reports"));

        if (SecurityUtils.hasRole(UserRole.ADMIN)) {
            tabs.add(createTab("Paramètres", VaadinIcon.COG, "settings"));
        }

        tabs.addSelectedChangeListener(event -> {
            Tab selectedTab = event.getSelectedTab();
            if (selectedTab != null) {
                String route = selectedTab.getElement().getAttribute("data-route");
                if (route != null) {
                    getUI().ifPresent(ui -> ui.navigate(route));
                }
            }
        });

        drawerLayout.add(drawerHeader, tabs);
        drawerLayout.setFlexGrow(1, tabs);

        return drawerLayout;
    }

    private Tab createTab(String label, VaadinIcon icon, String route) {
        HorizontalLayout tabContent = new HorizontalLayout();
        tabContent.setSpacing(true);
        tabContent.setAlignItems(FlexComponent.Alignment.CENTER);
        tabContent.setPadding(true);
        tabContent.getStyle()
                .set("cursor", "pointer")
                .set("border-radius", "8px")
                .set("transition", "all 0.3s ease")
                .set("width", "100%");

        Icon tabIcon = icon.create();
        tabIcon.setSize("20px");
        tabIcon.getStyle().set("color", "#64748b");

        Span tabLabel = new Span(label);
        tabLabel.getStyle()
                .set("color", "#334155")
                .set("font-weight", "500")
                .set("font-size", "14px");

        tabContent.add(tabIcon, tabLabel);
        tabContent.setFlexGrow(1, tabLabel);

        tabContent.getElement().executeJs(
                "this.addEventListener('mouseenter', () => {" +
                        "  this.style.backgroundColor = '#e0e7ff';" +
                        "  this.style.transform = 'translateX(4px)';" +
                        "});" +
                        "this.addEventListener('mouseleave', () => {" +
                        "  this.style.backgroundColor = 'transparent';" +
                        "  this.style.transform = 'translateX(0)';" +
                        "});"
        );

        Tab tab = new Tab();
        tab.add(tabContent);
        tab.getElement().setAttribute("data-route", route);
        tab.getStyle()
                .set("background-color", "transparent")
                .set("padding", "0")
                .set("margin", "0.25rem 0");

        return tab;
    }

    private String getCurrentUserDisplayName() {
        return SecurityUtils.getCurrentUser()
                .map(user -> user.getFirstName() + " " + user.getLastName())
                .orElse("Utilisateur");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Hook pour la navigation
    }
}