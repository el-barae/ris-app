package com.application.config;

import com.vaadin.flow.server.VaadinSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextListener;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

@Configuration
public class SessionConfig {

    @Bean
    public HttpSessionListener httpSessionListener() {
        return new HttpSessionListener() {
            @Override
            public void sessionCreated(HttpSessionEvent se) {
                // Session created - no action needed
            }

            @Override
            public void sessionDestroyed(HttpSessionEvent se) {
                // Clean up Vaadin session when HTTP session is destroyed
                VaadinSession vaadinSession = VaadinSession.getCurrent();
                if (vaadinSession != null) {
                    vaadinSession.close();
                }
            }
        };
    }

    @Bean
    public RequestContextListener requestContextListener() {
        return new RequestContextListener();
    }
}
