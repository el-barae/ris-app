package com.application.security;

import com.application.entity.User;
import com.application.entity.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
public class SecurityUtils {

    public static Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
            authentication.getPrincipal() instanceof String) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return Optional.of(((CustomUserDetails) principal).getUser());
        }
        
        return Optional.empty();
    }

    public static boolean isUserLoggedIn() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        // Vérifie que ce n'est pas l'utilisateur anonyme
        return !(authentication.getPrincipal() instanceof String);
    }

    public static boolean hasRole(UserRole role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role.name()));
    }

    public static boolean hasAnyRole(UserRole... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        
        return Arrays.stream(roles)
                .anyMatch(role -> hasRole(role));
    }

    public static String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        if (authentication.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) authentication.getPrincipal()).getUsername();
        }
        
        if (authentication.getPrincipal() instanceof UserDetails) {
            return ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        
        // Cas où le principal est juste le username (String)
        if (authentication.getPrincipal() instanceof String) {
            return (String) authentication.getPrincipal();
        }
        
        return null;
    }

    public static void logout() {
        // Nettoyer complètement le contexte de sécurité
        SecurityContextHolder.clearContext();
        
        // Forcer la redirection vers login pour nettoyer complètement l'état
        try {
            com.vaadin.flow.component.UI.getCurrent().getPage().executeJs(
                "localStorage.clear();" +
                "sessionStorage.clear();" +
                "window.location.href = '/login';"
            );
        } catch (Exception e) {
            // Ignorer les erreurs UI
        }
    }

    // Méthodes utilitaires supplémentaires
    
    public static Long getCurrentUserId() {
        return getCurrentUser().map(User::getId).orElse(null);
    }

    public static UserRole getCurrentUserRole() {
        return getCurrentUser().map(User::getRole).orElse(null);
    }

    public static boolean isAdmin() {
        return hasRole(UserRole.ADMIN);
    }

    public static boolean isMedecin() {
        return hasRole(UserRole.MEDECIN);
    }

    public static boolean isRadiologue() {
        return hasRole(UserRole.RADIOLOGUE);
    }

    public static boolean isTechnicien() {
        return hasRole(UserRole.TECHNICIEN);
    }

    public static boolean isSecretaire() {
        return hasRole(UserRole.SECRETAIRE);
    }

    public static boolean canAccessPatientData() {
        return hasAnyRole(UserRole.ADMIN, UserRole.MEDECIN, UserRole.RADIOLOGUE, UserRole.TECHNICIEN, UserRole.SECRETAIRE);
    }

    public static boolean canCreateReports() {
        return hasAnyRole(UserRole.RADIOLOGUE);
    }

    public static boolean canValidateReports() {
        return hasAnyRole(UserRole.RADIOLOGUE);
    }

    public static boolean canManageUsers() {
        return hasAnyRole(UserRole.ADMIN);
    }

    public static boolean canManageExams() {
        return hasAnyRole(UserRole.ADMIN, UserRole.MEDECIN, UserRole.SECRETAIRE);
    }

    public static boolean canAccessExamView() {
        return hasAnyRole(UserRole.ADMIN, UserRole.MEDECIN, UserRole.SECRETAIRE);
    }

    public static boolean canAccessSchedulingView() {
        return hasAnyRole(UserRole.ADMIN, UserRole.MEDECIN, UserRole.SECRETAIRE);
    }

    public static boolean canAccessWorklist() {
        return hasAnyRole(UserRole.ADMIN, UserRole.TECHNICIEN);
    }

    public static boolean canAccessReports() {
        return hasAnyRole(UserRole.ADMIN, UserRole.RADIOLOGUE);
    }
}
