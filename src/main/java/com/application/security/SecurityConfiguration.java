package com.application.security;

import static com.vaadin.flow.spring.security.VaadinSecurityConfigurer.vaadin;

import com.application.views.LoginView;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.core.Authentication;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Configuration
public class SecurityConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // Autoriser l'accès au WebSocket AVANT la configuration Vaadin
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/ws-exam-status/**").permitAll()
        );

        // Désactiver CSRF pour le WebSocket
        http.csrf(csrf -> csrf
                .ignoringRequestMatchers("/ws-exam-status/**")
        );

        http.with(vaadin(), vaadin -> vaadin
                .loginView(LoginView.class)
                .defaultSuccessUrl("/dashboard")
        );

        return http.build();
    }
}