package com.application.views;

import com.application.security.SecurityUtils;
import com.application.entity.UserRole;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Image;
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
import com.vaadin.flow.component.ClientCallable;
import org.springframework.beans.factory.annotation.Autowired;
import com.application.entity.Exam;
import com.application.repository.ExamRepository;

@Component
@UIScope
@PermitAll
public class MainLayout extends AppLayout implements BeforeEnterObserver {

    @Autowired
    private ExamRepository examRepository;

    public MainLayout() {
        setPrimarySection(Section.DRAWER);
        addToNavbar(createHeader());
        addToDrawer(createDrawer());
        
        // N'exécuter le WebSocket que si l'utilisateur est connecté
        if (SecurityUtils.isUserLoggedIn()) {
            setupWebSocketListener();
        }
    }

//    private void setupWebSocketListener() {
//        getElement().executeJs(
//                "function loadScript(src) {" +
//                        "  return new Promise((resolve, reject) => {" +
//                        "    if (document.querySelector('script[src=\"' + src + '\"]')) {" +
//                        "      console.log('✅ Script déjà chargé:', src);" +
//                        "      resolve();" +
//                        "      return;" +
//                        "    }" +
//                        "    const script = document.createElement('script');" +
//                        "    script.src = src;" +
//                        "    script.onload = () => {" +
//                        "      console.log('✅ Script chargé:', src);" +
//                        "      resolve();" +
//                        "    };" +
//                        "    script.onerror = (err) => {" +
//                        "      console.error('❌ Erreur chargement script:', src, err);" +
//                        "      reject(err);" +
//                        "    };" +
//                        "    document.head.appendChild(script);" +
//                        "  });" +
//                        "}" +
//                        "" +
//                        "function waitForGlobal(globalName, timeout = 5000) {" +
//                        "  return new Promise((resolve, reject) => {" +
//                        "    const startTime = Date.now();" +
//                        "    const checkInterval = setInterval(() => {" +
//                        "      if (typeof window[globalName] !== 'undefined') {" +
//                        "        console.log('✅ ' + globalName + ' disponible');" +
//                        "        clearInterval(checkInterval);" +
//                        "        resolve();" +
//                        "      } else if (Date.now() - startTime > timeout) {" +
//                        "        clearInterval(checkInterval);" +
//                        "        reject(new Error(globalName + ' non disponible après ' + timeout + 'ms'));" +
//                        "      }" +
//                        "    }, 50);" +
//                        "  });" +
//                        "}" +
//                        "" +
//                        "loadScript('https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js')" +
//                        ".then(() => waitForGlobal('SockJS'))" +
//                        ".then(() => loadScript('https://cdn.jsdelivr.net/npm/@stomp/stompjs@5/bundles/stomp.umd.min.js'))" +
//                        ".then(() => waitForGlobal('StompJs'))" +
//                        ".then(() => {" +
//                        "  console.log('📚 Librairies WebSocket chargées');" +
//                        "  " +
//                        "  const socket = new SockJS('/ws-exam-status');" +
//                        "  const stompClient = StompJs.Stomp.over(socket);" +
//                        "  " +
//                        "  stompClient.connect({}, function(frame) {" +
//                        "    console.log('✅ WebSocket connecté:', frame);" +
//                        "    " +
//                        "    stompClient.subscribe('/topic/exam-status', function(message) {" +
//                        "      try {" +
//                        "        const data = JSON.parse(message.body);" +
//                        "        console.log('📨 Message MPPS reçu:', data);" +
//                        "        " +
//                        "        $0.$server.showStatusDialog(" +
//                        "          data.accessionNumber," +
//                        "          data.patientName," +
//                        "          data.examType," +
//                        "          data.newStatus," +
//                        "          data.message" +
//                        "        );" +
//                        "      } catch (error) {" +
//                        "        console.error('❌ Erreur traitement message:', error);" +
//                        "      }" +
//                        "    });" +
//                        "  }, function(error) {" +
//                        "    console.error('❌ Erreur connexion WebSocket:', error);" +
//                        "  });" +
//                        "  " +
//                        "  window._stompClient = stompClient;" +
//                        "  " +
//                        "  window.addEventListener('beforeunload', function() {" +
//                        "    if (window._stompClient && window._stompClient.connected) {" +
//                        "      console.log('🔌 Déconnexion WebSocket');" +
//                        "      window._stompClient.disconnect();" +
//                        "    }" +
//                        "  });" +
//                        "})" +
//                        ".catch(error => {" +
//                        "  console.error('❌ Erreur chargement librairies WebSocket:', error);" +
//                        "});"
//        );
//    }

    private void setupWebSocketListener() {
        getElement().executeJs(
                "console.log('🚀 Initialisation WebSocket...');" +
                        "" +
                        // Vérifier si déjà initialisé
                        "if (window._wsInitialized) {" +
                        "  console.log('⚠️  WebSocket déjà initialisé, abandon');" +
                        "  return;" +
                        "}" +
                        "window._wsInitialized = true;" +
                        "" +
                        "function loadScript(src) {" +
                        "  return new Promise((resolve, reject) => {" +
                        "    if (document.querySelector('script[src=\"' + src + '\"]')) {" +
                        "      console.log('✅ Script déjà présent:', src);" +
                        "      setTimeout(resolve, 50);" +
                        "      return;" +
                        "    }" +
                        "    const script = document.createElement('script');" +
                        "    script.src = src;" +
                        "    script.onload = () => {" +
                        "      console.log('✅ Script chargé:', src);" +
                        "      setTimeout(resolve, 100);" +
                        "    };" +
                        "    script.onerror = (err) => {" +
                        "      console.error('❌ Erreur chargement:', src, err);" +
                        "      reject(err);" +
                        "    };" +
                        "    document.head.appendChild(script);" +
                        "  });" +
                        "}" +
                        "" +
                        "loadScript('https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js')" +
                        "  .then(() => loadScript('https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js'))" +
                        "  .then(() => {" +
                        "    console.log('📚 Librairies chargées - Vérification...');" +
                        "    " +
                        "    if (typeof SockJS === 'undefined') {" +
                        "      throw new Error('SockJS non disponible');" +
                        "    }" +
                        "    if (typeof Stomp === 'undefined') {" +
                        "      throw new Error('Stomp non disponible');" +
                        "    }" +
                        "    " +
                        "    if (window._stompClient && window._stompClient.connected) {" +
                        "      console.log('⚠️  Déconnexion de l\\'ancienne connexion');" +
                        "      window._stompClient.disconnect();" +
                        "    }" +
                        "    " +
                        "    console.log('✅ SockJS et Stomp disponibles');" +
                        "    console.log('🔌 Connexion au WebSocket...');" +
                        "    " +
                        "    const socket = new SockJS('/ws-exam-status');" +
                        "    const stompClient = Stomp.over(socket);" +
                        "    " +
                        "    stompClient.debug = function(str) {" +
                        "      console.log('📝 STOMP:', str);" +
                        "    };" +
                        "    stompClient.connect({}, function(frame) {" +
                        "      console.log('✅ WebSocket connecté!', frame);" +
                        "      console.log('👂 Abonnement à /topic/exam-status...');" +
                        "      const processedMessages = new Map();" +
                        "      const DEDUP_WINDOW_MS = 5000;" + // 5 secondes
                        "      " +
                        "      const subscription = stompClient.subscribe('/topic/exam-status', function(message) {" +
                        "        console.log('📨 Message WebSocket reçu!');" +
                        "        console.log('📦 Timestamp:', new Date().toISOString());" +
                        "        console.log('📦 Message brut:', message.body);" +
                        "        " +
                        "        try {" +
                        "          const data = JSON.parse(message.body);" +
                        "          console.log('📊 Données parsées:', data);" +
                        "          " +
                        "          const messageKey = data.accessionNumber + '_' + data.newStatus;" +
                        "          const now = Date.now();" +
                        "          " +
                        "          const lastProcessed = processedMessages.get(messageKey);" +
                        "          if (lastProcessed && (now - lastProcessed) < DEDUP_WINDOW_MS) {" +
                        "            console.warn('⚠️  Message dupliqué ignoré:', messageKey);" +
                        "            return;" +
                        "          }" +
                        "          " +
                        "          processedMessages.set(messageKey, now);" +
                        "          " +
                        "          for (const [key, timestamp] of processedMessages.entries()) {" +
                        "            if (now - timestamp > DEDUP_WINDOW_MS) {" +
                        "              processedMessages.delete(key);" +
                        "            }" +
                        "          }" +
                        "          " +
                        "          if (!$0 || !$0.$server) {" +
                        "            console.error('❌ $0.$server non disponible');" +
                        "            return;" +
                        "          }" +
                        "          " +
                        "          console.log('📞 Appel showStatusDialog...');" +
                        "          $0.$server.showStatusDialog(" +
                        "            data.accessionNumber," +
                        "            data.patientName," +
                        "            data.examType," +
                        "            data.newStatus," +
                        "            data.message" +
                        "          );" +
                        "          console.log('✅ showStatusDialog appelé avec succès');" +
                        "        } catch (error) {" +
                        "          console.error('❌ Erreur traitement message:', error);" +
                        "          console.error('Stack:', error.stack);" +
                        "        }" +
                        "      });" +
                        "      " +
                        "      console.log('✅ Abonnement actif:', subscription.id);" +
                        "      window._subscription = subscription;" +
                        "    }, function(error) {" +
                        "      console.error('❌ Erreur connexion WebSocket:', error);" +
                        "      window._wsInitialized = false;" + // Permettre retry en cas d'erreur
                        "    });" +
                        "    " +
                        "    window._stompClient = stompClient;" +
                        "    console.log('💾 Client STOMP stocké dans window._stompClient');" +
                        "    " +
                        "    window.addEventListener('beforeunload', function() {" +
                        "      if (window._stompClient && window._stompClient.connected) {" +
                        "        console.log('🔌 Déconnexion WebSocket');" +
                        "        window._stompClient.disconnect();" +
                        "      }" +
                        "    });" +
                        "  })" +
                        "  .catch(error => {" +
                        "    console.error('❌ Erreur fatale WebSocket:', error);" +
                        "    console.error('Stack:', error.stack);" +
                        "    window._wsInitialized = false;" + // Permettre retry en cas d'erreur
                        "  });"
        );
    }

    @ClientCallable
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

            Button viewerButton = new Button("Voir dans le viewer", VaadinIcon.EXTERNAL_LINK.create());
            viewerButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            viewerButton.addClickListener(e -> {
                dialog.close();
                // Redirection vers le viewer OHIF avec l'accession number
                String viewerUrl = "http://localhost/viewer?StudyInstanceUIDs=" + getStudyInstanceUID(accessionNumber);
                getUI().ifPresent(ui -> ui.getPage().open(viewerUrl, "_blank"));
            });

            Button closeButton = new Button("Fermer");
            closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            closeButton.addClickListener(e -> dialog.close());

            // Ajouter le bouton viewer uniquement pour les messages de réception PACS
            if ("Images reçues et enregistrées dans le PACS central".equals(message)) {
                footer.add(closeButton, refreshButton, viewerButton);
            } else {
                footer.add(closeButton, refreshButton);
            }

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
            icon.setColor("#6b7280");
        } else if ("COMPLETED".equals(status)) {
            icon = VaadinIcon.CHECK_CIRCLE.create();
            icon.setColor("#10b981");
        } else {
            icon = VaadinIcon.CLOSE_CIRCLE.create();
            icon.setColor("#7f1d1d");
        }
        return icon;
    }

    private String getStatusColor(String status) {
        switch (status) {
            case "IN_PROGRESS": return "#6b7280";
            case "COMPLETED": return "#10b981";
            case "CANCELLED": return "#7f1d1d";
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
        Notification notification = new Notification(message, 4000, Notification.Position.TOP_END);

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
                .set("background", "linear-gradient(135deg, #059669 0%, #047857 100%)")
                .set("color", "white")
                .set("box-shadow", "0 2px 8px rgba(0,0,0,0.1)")
                .set("min-height", "60px")
                .set("padding-left", "20px")
                .set("padding-right", "20px");

        Button menuToggle = new Button(VaadinIcon.MENU.create());
        menuToggle.addClickListener(e -> setDrawerOpened(!isDrawerOpened()));
        menuToggle.addClassNames("menu-toggle");
        menuToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        menuToggle.getStyle()
                .set("color", "white")
                .set("background", "rgba(255,255,255,0.1)")
                .set("margin-top", "14px");
        header.add(menuToggle);

        H2 title = new H2();
        title.addClassNames("text-truncate", "m-0");
        title.getStyle()
                .set("color", "white")
                .set("font-size", "1.5rem")
                .set("font-weight", "600")
                .set("margin-top", "8px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "12px");

        Image logoImage = new Image("images/sahty.jpeg", "RIS Sahty Logo");
        logoImage.setHeight("40px");
        logoImage.setWidth("40px");
        logoImage.getStyle()
                .set("border-radius", "8px")
                .set("flex-shrink", "0")
                .set("margin-right", "8px")
                .set("margin-left", "8px");
        
        title.add(logoImage);
        title.add("RIS Sahty");

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
            // Nettoyer complètement avant déconnexion
            SecurityUtils.logout();
            
            // Forcer la redirection après un court délai pour s'assurer que tout est nettoyé
            getUI().ifPresent(ui -> {
                ui.getPage().executeJs(
                    "setTimeout(function() {" +
                    "  window.location.href = '/login';" +
                    "}, 100);"
                );
            });
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
                .set("background-color", "#f5f5f5")
                .set("padding-top", "1rem");

        HorizontalLayout drawerHeader = new HorizontalLayout();
        drawerHeader.setWidthFull();
        drawerHeader.setPadding(true);
        drawerHeader.setSpacing(true);
        drawerHeader.getStyle()
                .set("border-bottom", "2px solid #6b7280")
                .set("margin-bottom", "1rem");

        Icon menuIcon = VaadinIcon.MENU.create();
        menuIcon.setSize("24px");
        menuIcon.getStyle().set("color", "#10b981");

        H3 drawerTitle = new H3("Menu");
        drawerTitle.getStyle()
                .set("margin", "0")
                .set("color", "#374151")
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
        
        // ExamView - accessible par ADMIN, MEDECIN, SECRETAIRE
        if (SecurityUtils.canAccessExamView()) {
            tabs.add(createTab("Reception", VaadinIcon.CLIPBOARD_TEXT, "exams"));
        }
        
        // SchedulingView - accessible par ADMIN, MEDECIN, SECRETAIRE
        if (SecurityUtils.canAccessSchedulingView()) {
            tabs.add(createTab("Planification", VaadinIcon.CALENDAR, "scheduling"));
        }
        
        // WorklistDragDrop - accessible par ADMIN, TECHNICIEN
        if (SecurityUtils.canAccessWorklist()) {
            tabs.add(createTab("WorkList", VaadinIcon.LIST, "worklist-dragdrop"));
        }
        
        // ReportView - accessible par ADMIN, RADIOLOGUE
        if (SecurityUtils.canAccessReports()) {
            tabs.add(createTab("Rapports", VaadinIcon.FILE_TEXT, "reports"));
        }

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
        tabIcon.getStyle().set("color", "#6b7280");

        Span tabLabel = new Span(label);
        tabLabel.getStyle()
                .set("color", "#374151")
                .set("font-weight", "500")
                .set("font-size", "14px");

        tabContent.add(tabIcon, tabLabel);
        tabContent.setFlexGrow(1, tabLabel);

        tabContent.getElement().executeJs(
                "this.addEventListener('mouseenter', () => {" +
                        "  this.style.backgroundColor = '#f5f5f5';" +
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
        // Configurer le WebSocket uniquement si l'utilisateur est connecté et sur une page protégée
        if (SecurityUtils.isUserLoggedIn() && !event.getLocation().getPath().equals("/login")) {
            setupWebSocketListener();
        }
    }

    private String getStudyInstanceUID(String accessionNumber) {
        try {
            return examRepository.findByAccessionNumber(accessionNumber)
                    .map(Exam::getStudyInstanceUID)
                    .orElse("");
        } catch (Exception e) {
            System.err.println("Erreur récupération StudyInstanceUID: " + e.getMessage());
            return "";
        }
    }
}