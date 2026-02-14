package com.application.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.vaadin.flow.server.VaadinSession;
import com.application.entity.User;
import com.application.repository.UserRepository;

@Service
public class VaadinAuthService {

    @Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    public boolean authenticate(String username, String password) {
        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            if (passwordEncoder.matches(password, userDetails.getPassword())) {
                // Créer et établir l'authentification
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, 
                    null, 
                    userDetails.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                
                // Stocker l'utilisateur dans la session Vaadin pour accès facile
                if (userDetails instanceof CustomUserDetails) {
                    // Recharger l'utilisateur avec l'hôpital pour éviter les problèmes de lazy loading
                    User fullUser = userRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("User not found after auth"));
                    VaadinSession.getCurrent().setAttribute("user", fullUser);
                    System.out.println("DEBUG: User stored in Vaadin session: " + fullUser.getUsername() + ", hospital: " + fullUser.getHospital());
                }
                
                return true;
            }
            return false;
        } catch (Exception e) {
            System.out.println("DEBUG: Authentication error: " + e.getMessage());
            return false;
        }
    }
}
