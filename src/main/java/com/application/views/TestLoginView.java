package com.application.views;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("test-login")
@PageTitle("Test Login")
@AnonymousAllowed
public class TestLoginView extends VerticalLayout {

    public TestLoginView() {
        add(new H1("Test Login Page"));
        add(new Paragraph("This is a simple test login page to verify routing works."));
        add(new Paragraph("If you can see this, the routing is working correctly."));
    }
}
