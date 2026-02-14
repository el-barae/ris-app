package com.application.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Configuration
public class SecurityConfiguration {

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http, UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) throws Exception {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return new org.springframework.security.authentication.ProviderManager(authProvider);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, DaoAuthenticationProvider authProvider) throws Exception {
        // Autoriser l'accès aux ressources publiques
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/ws-exam-status/**").permitAll()
                .requestMatchers("/login", "/login/**", "/test-login").permitAll()
                .requestMatchers("/perform-login").permitAll()
                .requestMatchers("/images/**", "/icons/**", "/frontend/**", "/styles/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/webjars/**").permitAll()
                .requestMatchers("/VAADIN/**").permitAll()
                .requestMatchers("/vite-dev-server/**").permitAll()
                .requestMatchers("/*.js").permitAll()
                .requestMatchers("/*.css").permitAll()
                .requestMatchers("/*.html").permitAll()
                .requestMatchers("/favicon.ico").permitAll()
                .requestMatchers("/manifest.json").permitAll()
                .anyRequest().permitAll()  // Allow all requests - Vaadin will handle auth
        );

        // Désactiver le formulaire de login Spring - Vaadin gère l'authentification
        http.formLogin(form -> form.disable());

        // Configuration logout
        http.logout(logout -> logout
                .logoutSuccessUrl("/login")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
        );

        // Configurer l'authentication provider
        http.authenticationProvider(authProvider);

        // Désactiver CSRF pour Vaadin
        http.csrf(csrf -> csrf.disable());

        return http.build();
    }
}