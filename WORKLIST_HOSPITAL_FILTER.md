# Filtrage par Hôpital dans WorklistDragDropView

## Overview
Implémentation du filtrage des examens par hôpital de l'utilisateur connecté dans la vue WorklistDragDropView (Worklist DICOM MWL).

## Fonctionnalité

### 1. Filtrage Automatique
- **Examens planifiés**: Seuls les examens de l'hôpital de l'utilisateur sont affichés
- **Examens actifs**: Seuls les examens sélectionnés de l'hôpital de l'utilisateur sont affichés
- **Sécurité**: L'utilisateur ne voit que les données de son établissement dans la worklist

### 2. Mécanisme de Filtrage

#### Récupération de l'Hôpital
```java
private Hospital getCurrentUserHospital() {
    try {
        User currentUser = (User) VaadinSession.getCurrent().getAttribute("user");
        if (currentUser != null && currentUser.getHospital() != null) {
            return currentUser.getHospital();
        }
    } catch (Exception e) {
        // En cas d'erreur, retourner null
    }
    return null;
}
```

#### Filtrage dans refreshGrids()
```java
private void refreshGrids() {
    // Récupérer l'hôpital de l'utilisateur connecté
    Hospital userHospital = getCurrentUserHospital();
    
    // Charger les examens planifiés filtrés par hôpital
    if (userHospital != null) {
        allPlannedExams = examRepo.findByStatusAndHospital(ExamStatus.PLANNED, userHospital);
    } else {
        // Si pas d'hôpital, retourner une liste vide
        allPlannedExams = new ArrayList<>();
    }
    displayPlannedExams(allPlannedExams);
    plannedCount.setText(String.valueOf(allPlannedExams.size()));

    // Charger les examens actifs filtrés par hôpital
    List<Exam> activeExams;
    if (userHospital != null) {
        activeExams = examRepo.findByStatusAndHospital(ExamStatus.SELECTED, userHospital);
    } else {
        activeExams = new ArrayList<>();
    }
    rightGrid.setItems(activeExams);
    inProgressCount.setText(String.valueOf(activeExams.size()));
}
```

## Modifications Techniques

### 1. Imports Ajoutés
```java
import com.application.entity.User;
import com.application.entity.Hospital;
import com.vaadin.flow.server.VaadinSession;
```

### 2. Méthodes Ajoutées

#### getCurrentUserHospital()
- **Rôle**: Récupérer l'hôpital de l'utilisateur connecté depuis la session
- **Sécurité**: Gestion des erreurs et retour null si problème
- **Session**: Utilisation de VaadinSession pour obtenir l'utilisateur courant

#### refreshGrids() modifiée
- **Filtrage**: Application du filtrage par hôpital pour les deux listes
- **Sécurité**: Listes vides si pas d'hôpital configuré
- **Performance**: Utilisation des méthodes de repository optimisées

### 3. Repository Utilisé
```java
// Méthode déjà existante dans ExamRepository
List<Exam> findByStatusAndHospital(ExamStatus status, Hospital hospital);
```

## Workflow Utilisateur

### 1. Accès à la Worklist
```
Utilisateur: Technicien (Hôpital Central)
├── Accède à /worklist-dragdrop
├── Vue: "Worklist DICOM (MWL)"
├── Filtre automatique: Hôpital Central ✓
└── Voir uniquement les examens de son hôpital
```

### 2. Interface Drag & Drop
```
Côté Gauche - Examens en Attente:
├── CT Thoracique - Patient A (Hôpital Central)
├── IRM Cérébrale - Patient B (Hôpital Central)
├── Radio Abdomen - Patient C (Hôpital Central)
└── (Pas d'examens des autres hôpitaux) ✓

Côté Droit - Examens à Envoyer vers MWL:
├── Examens sélectionnés (Hôpital Central)
├── Bouton "Envoyer à MWL"
└── (Uniquement les examens de l'hôpital) ✓
```

### 3. Sécurité Multi-Hôpitaux
```
Utilisateur A: Technicien (Hôpital Central)
├── Voit: Examens de l'Hôpital Central uniquement ✓
├── Peut: Drag & Drop des examens de son hôpital
├── Ne voit pas: Examens de l'Hôpital Nord ✓
└── Ne peut pas: Manipuler les examens d'autres hôpitaux ✓

Utilisateur B: Technicien (Hôpital Nord)
├── Voit: Examens de l'Hôpital Nord uniquement ✓
├── Peut: Envoyer à MWL les examens de son hôpital
├── Ne voit pas: Examens de l'Hôpital Central ✓
└── Ne peut pas: Accéder aux données d'autres hôpitaux ✓
```

## Avantages

### 1. Sécurité des Données
- **Isolation**: Chaque utilisateur voit uniquement ses examens
- **Contrôle d'accès**: Pas de risque de manipulation inter-hôpital
- **Audit**: Traçabilité claire des actions par établissement

### 2. Expérience Utilisateur
- **Simplicité**: Interface ciblée sur les données pertinentes
- **Pertinence**: Moins de données à traiter, affichage plus rapide
- **Clarté**: Pas de confusion entre les examens de différents hôpitaux

### 3. Gestion Multi-Établissements
- **Scalabilité**: Supporte plusieurs hôpitaux sans configuration complexe
- **Organisation**: Chaque hôpital gère ses propres worklists
- **Administration**: Gestion centralisée par rôle et hôpital

## Cas d'Usage

### 1. Technicien DICOM
```
Technicien: Pierre (Hôpital Central)
├── Ouvre la worklist DICOM
├── Voit 15 examens en attente (Hôpital Central)
├── Sélectionne 5 examens pour la worklist
├── Glisse-dépose vers la zone MWL
├── Clique "Envoyer à MWL"
└── Seuls les 5 examens de son hôpital sont envoyés ✓
```

### 2. Administrateur Système
```
Admin: Système (Multi-hôpitaux)
├── Peut voir toutes les worklists (si configuré)
├── Gère les permissions par hôpital
├── Supervise l'activité de worklist
└── Audit des envois MWL par établissement
```

### 3. Workflow Quotidien
```
Matin - Hôpital Central:
├── 20 examens planifiés disponibles
├── 8 examens déjà dans la worklist
├── Technicien ajoute 3 examens
├── Envoi MWL de 11 examens
└── Archive automatique des examens envoyés

Matin - Hôpital Nord:
├── 15 examens planifiés disponibles
├── 5 examens déjà dans la worklist
├── Technicien ajoute 2 examens
├── Envoi MWL de 7 examens
└── Archive automatique des examens envoyés
```

## Gestion des Erreurs

### 1. Utilisateur Sans Hôpital
```
Erreur: Aucun examen affiché
Cause: L'utilisateur n'a pas d'hôpital configuré
Solution: Configurer l'hôpital dans le profil utilisateur
Résultat: Listes vides avec message approprié
```

### 2. Session Expirée
```
Erreur: Impossible de récupérer l'hôpital
Cause: Session utilisateur invalide
Solution: Reconnexion requise
Résultat: Interface vide jusqu'à reconnexion
```

### 3. Hôpital Inexistant
```
Erreur: Aucun examen trouvé
Cause: L'hôpital de l'utilisateur n'existe plus
Solution: Mettre à jour le profil utilisateur
Résultat: Listes vides, notification d'erreur
```

## Impact sur le Code Existant

### 1. Compatibilité
- **Repository**: Utilisation de méthodes existantes
- **Interface**: Aucun changement visuel pour l'utilisateur
- **Workflow**: Drag & Drop fonctionne normalement

### 2. Performance
- **Chargement**: Moins de données à charger et afficher
- **Requêtes**: Requêtes SQL optimisées avec filtrage
- **Interface**: Affichage plus rapide et plus pertinent

### 3. Sécurité
- **Contrôle**: Validation automatique par hôpital
- **Permissions**: Basées sur les rôles et l'hôpital
- **Audit**: Historique clair des actions par établissement

## Configuration Requise

### 1. Entité User
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "hospital_id")
private Hospital hospital;
```

### 2. Session Utilisateur
```java
// Dans le processus d'authentification
VaadinSession.getCurrent().setAttribute("user", authenticatedUser);
```

### 3. Permissions
```java
@RolesAllowed({"ADMIN", "TECHNICIEN"})
public class WorklistDragDropView extends VerticalLayout {
    // La vue est déjà configurée pour les rôles appropriés
}
```

## Bonnes Pratiques

### 1. Validation
- Toujours vérifier que l'hôpital est disponible
- Gérer les cas où l'utilisateur n'a pas d'hôpital
- Fournir des messages d'erreur clairs

### 2. Performance
- Mettre en cache l'hôpital de l'utilisateur si nécessaire
- Éviter les requêtes répétitives
- Optimiser les requêtes SQL

### 3. Sécurité
- Ne jamais permettre de contourner le filtrage
- Valider que l'utilisateur a les droits sur son hôpital
- Logger les tentatives d'accès non autorisés

## Évolutions Futures Possibles

1. **Multi-hôpitaux**: Permettre aux utilisateurs de gérer plusieurs hôpitaux
2. **Permissions avancées**: Contrôles plus granulaires par rôle et hôpital
3. **Audit complet**: Journalisation détaillée des actions MWL par hôpital
4. **Interface admin**: Interface pour gérer les associations utilisateur-hôpital
5. **Notifications**: Alertes pour les worklists par hôpital
6. **Export**: Export MWL filtré par hôpital

Cette fonctionnalité assure que chaque technicien ne voit et ne peut manipuler que les examens de son propre hôpital dans la worklist DICOM, garantissant la sécurité, la pertinence et la performance de l'interface de gestion des worklists.
