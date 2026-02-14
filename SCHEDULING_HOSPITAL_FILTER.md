# Filtrage par Hôpital dans SchedulingView

## Overview
Implémentation du filtrage des examens et créneaux par hôpital de l'utilisateur connecté dans la vue de planification (SchedulingView).

## Fonctionnalité

### 1. Filtrage Automatique
- **Examens à planifier**: Seuls les examens de l'hôpital de l'utilisateur sont affichés
- **Créneaux planifiés**: Seuls les créneaux de l'hôpital de l'utilisateur sont affichés
- **Sécurité**: L'utilisateur ne voit que les données de son établissement

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

#### Filtrage des Examens
```java
// ExamRepository
@Query("SELECT e FROM Exam e LEFT JOIN FETCH e.patient LEFT JOIN FETCH e.medecin LEFT JOIN FETCH e.report LEFT JOIN e.procedure LEFT JOIN FETCH e.order o WHERE e.status = :status AND o.hospital.id = :hospitalId")
List<Exam> findByStatusAndHospital(ExamStatus status, Hospital hospital);
```

#### Filtrage des Créneaux
```java
// ScheduleSlotRepository
@Query("SELECT s FROM ScheduleSlot s JOIN FETCH s.orderLine JOIN FETCH s.orderLine.patient JOIN FETCH s.orderLine.medecin JOIN FETCH s.modalityResource JOIN FETCH s.modalityResource.modalityType JOIN FETCH s.modalityResource.room WHERE s.status = :status AND s.orderLine.hospital.id = :hospitalId ORDER BY s.scheduledStartTime")
List<ScheduleSlot> findByStatusAndHospitalOrderByScheduledStartTime(ScheduleSlotStatus status, Hospital hospital);
```

## Modifications Techniques

### 1. Repositories

#### ExamRepository
```java
// Nouvelles méthodes pour filtrer par hôpital
@Query("SELECT e FROM Exam e LEFT JOIN FETCH e.patient LEFT JOIN e.medecin LEFT JOIN FETCH e.report LEFT JOIN e.procedure LEFT JOIN e.order o WHERE e.status = :status AND o.hospital.id = :hospitalId")
List<Exam> findByStatusAndHospital(ExamStatus status, Hospital hospital);

@Query("SELECT e FROM Exam e LEFT JOIN FETCH e.patient LEFT JOIN e.medecin LEFT JOIN e.report LEFT JOIN e.procedure LEFT JOIN e.order o WHERE o.hospital.id = :hospitalId")
List<Exam> findByHospital(Hospital hospital);
```

#### ScheduleSlotRepository
```java
@Query("SELECT s FROM ScheduleSlot s JOIN FETCH s.orderLine JOIN FETCH s.orderLine.patient JOIN FETCH s.orderLine.medecin JOIN FETCH s.modalityResource JOIN FETCH s.modalityResource.modalityType JOIN FETCH s.modalityResource.room WHERE s.status = :status AND s.orderLine.hospital.id = :hospitalId ORDER BY s.scheduledStartTime")
List<ScheduleSlot> findByStatusAndHospitalOrderByScheduledStartTime(ScheduleSlotStatus status, Hospital hospital);
```

### 2. SchedulingView

#### Méthode refreshScheduleList()
```java
private void refreshScheduleList() {
    // Récupérer l'hôpital de l'utilisateur connecté
    Hospital userHospital = getCurrentUserHospital();
    
    // Récupérer les examens à planifier (status CREATED) filtrés par hôpital
    List<Exam> examsToSchedule;
    if (userHospital != null) {
        examsToSchedule = examRepo.findByStatusAndHospital(ExamStatus.CREATED, userHospital);
    } else {
        // Si pas d'hôpital, retourner une liste vide
        examsToSchedule = List.of();
    }
    
    // Récupérer les créneaux planifiés (status SCHEDULED) filtrés par hôpital
    List<ScheduleSlot> scheduledSlots;
    if (userHospital != null) {
        scheduledSlots = scheduleSlotRepo.findByStatusAndHospitalOrderByScheduledStartTime(ScheduleSlotStatus.SCHEDULED, userHospital);
    } else {
        scheduledSlots = List.of();
    }
    
    // Combiner les deux listes
    List<Object> combinedList = new java.util.ArrayList<>();
    combinedList.addAll(examsToSchedule);
    combinedList.addAll(scheduledSlots);
    
    schedulingGrid.setItems(combinedList);
}
```

## Workflow Utilisateur

### 1. Accès à la Planification
```
Utilisateur: Dr. Martin (Hôpital Central)
├── Accède à /scheduling
├── Vue: "Planification des examens"
├── Filtre automatique: Hôpital Central ✓
└── Voir uniquement les examens de son hôpital
```

### 2. Planification d'Examens
```
1. Liste des examens à planifier
   ├── Exam 1: CT Thoracique (Hôpital Central)
   ├── Exam 2: IRM Cérébrale (Hôpital Central)
   └── Exam 3: Radio Abdomen (Hôpital Central)

2. Liste des créneaux disponibles
   ├── Salle 1: CT Scanner (Hôpital Central)
   ├── Salle 2: IRM Machine (Hôpital Central)
   └── Salle 3: Radio Appareil (Hôpital Central)

3. Actions possibles
   ├── Affecter un examen à un créneau
   ├── Créer un nouveau créneau
   └── Annuler un créneau
```

### 3. Sécurité Multi-Hôpitaux
```
Utilisateur A: Dr. Martin (Hôpital Central)
├── Voit: Examens de l'Hôpital Central uniquement
├── Ne peut pas voir: Examens de l'Hôpital Nord

Utilisateur B: Dr. Durand (Hôpital Nord)
├── Voit: Examens de l'Hôpital Nord uniquement
├── Ne peut pas voir: Examens de l'Hôpital Central
```

## Avantages

### 1. Sécurité des Données
- **Isolation**: Chaque utilisateur voit uniquement ses données
- **Contrôle d'accès**: Pas de risque de modification inter-hôpital
- **Audit**: Traçabilité claire par établissement

### 2. Expérience Utilisateur
- **Simplicité**: Pas besoin de filtrer manuellement
- **Pertinence**: Interface ciblée sur les données pertinentes
- **Performance**: Chargement plus rapide avec moins de données

### 3. Gestion Multi-Établissements
- **Scalabilité**: Supporte plusieurs hôpitaux
- **Organisation**: Chaque hôpital gère ses propres données
- **Administration**: Gestion centralisée par rôle

## Cas d'Usage

### 1. Secrétaire d'Hôpital
```
Secrétaire: Marie Dupont (Hôpital Central)
├── Planification du jour
│   ├── 8h00: CT Thoracique - Patient A
│   ├── 10h00: IRM Cérébrale - Patient B
│   ├── 14h00: Radio Abdomen - Patient C
│   └── 16h00: Échographie - Patient D
└── Créneaux disponibles pour demain
```

### 2. Médecin Spécialiste
```
Dr. Martin (Hôpital Central, Radiologue)
├── Consulte la planification
├── Voit les examens à planifier
├── Peut créer des créneaux
├── Ne voit pas les autres hôpitaux
```

### 3. Administrateur
```
Admin: Système (Multi-hôpitaux)
├── Peut voir toutes les données (si configuré)
├── Gère les permissions par hôpital
└── Supervise l'activité de planification
```

## Gestion des Erreurs

### 1. Utilisateur Sans Hôpital
```
Erreur: Aucun examen trouvé
Cause: L'utilisateur n'a pas d'hôpital configuré
Solution: Configurer l'hôpital dans le profil utilisateur
```

### 2. Session Expirée
```
Erreur: Impossible de récupérer l'hôpital
Cause: Session utilisateur invalide
Solution: Reconnexion requise
```

### 3. Hôpital Inexistant
```
Erreur: Aucun examen trouvé
Cause: L'hôpital de l'utilisateur n'existe plus
Solution: Mettre à jour le profil utilisateur
```

## Impact sur le Code Existant

### 1. Compatibilité
- **Repositories**: Ajout de méthodes de filtrage
- **Vues**: Adaptation pour le filtrage automatique
- **Services**: Aucun changement requis

### 2. Performance
- **Chargement**: Moins de données à charger
- **Requêtes**: Requêtes SQL optimisées
- **Interface**: Affichage plus rapide

### 3. Sécurité
- **Contrôle**: Validation par hôpital
- **Permissions**: Basées sur les rôles
- **Audit**: Historique par établissement

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
@RolesAllowed({"ADMIN", "MEDECIN", "SECRETAIRE"})
public class SchedulingView extends VerticalLayout {
    // La vue est déjà configurée pour les rôles appropriés
}
```

## Bonnes Pratiques

### 1. Validation
- Toujours vérifier que l'hôpital est disponible
- Gérer les cas où l'utilisateur n'a pas d'hôpital
- Fournir des messages d'erreur clairs

### 2. Performance
- Mettre en cache l'hôpital de l'utilisateur
- Éviter les requêtes répétitives
- Optimiser les requêtes SQL

### 3. Sécurité
- Ne jamais permettre de contourner le filtrage
- Valider que l'utilisateur a les droits sur son hôpital
- Logger les tentatives d'accès non autorisés

## Évolutions Futures Possibles

1. **Multi-hôpitaux**: Permettre aux utilisateurs de gérer plusieurs hôpitaux
2. **Permissions avancées**: Contrôles plus granulaires par rôle et hôpital
3. **Audit complet**: Journalisation détaillée des actions par hôpital
4. **Interface admin**: Interface pour gérer les associations utilisateur-hôpital
5. **Notifications**: Alertes pour les examens urgents par hôpital

Cette fonctionnalité assure que chaque utilisateur ne voit et ne peut planifier que les examens et créneaux de son propre hôpital, garantissant la sécurité et la pertinence des données affichées.
