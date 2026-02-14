# Hôpital Automatique dans OrderView

## Overview
Implémentation de l'affectation automatique de l'hôpital de l'utilisateur connecté lors de la création d'ordres dans OrderView.

## Fonctionnalité

### 1. Champ Hôpital Masqué
- **Suppression**: Le champ de sélection d'hôpital n'est plus affiché dans l'interface
- **Automatisation**: L'hôpital est automatiquement affecté depuis l'utilisateur connecté
- **Simplification**: L'utilisateur n'a plus besoin de sélectionner manuellement l'hôpital

### 2. Mécanisme d'Affectation

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

#### Affectation Automatique
- **Création d'ordre**: L'hôpital de l'utilisateur est automatiquement assigné
- **Modification**: L'hôpital original est conservé
- **Validation**: Vérification que l'hôpital est bien disponible

## Modifications Techniques

### 1. Interface Utilisateur

#### Formulaire Simplifié
```
Avant:
├── Hôpital: [Sélection manuelle]
├── Médecin: [Sélection]
└── Patient: [Sélection]

Après:
├── Médecin: [Sélection]
└── Patient: [Sélection]
(Hôpital automatique)
```

#### Code UI
```java
// Sélection hôpital (caché - sera affecté automatiquement)
ComboBox<Hospital> hospitalSelector = new ComboBox<>("Hôpital");
hospitalSelector.setVisible(false); // Cacher le champ

// Récupérer l'hôpital de l'utilisateur connecté
Hospital userHospital = getCurrentUserHospital();
if (userHospital != null) {
    hospitalSelector.setValue(userHospital);
}
```

### 2. Logique Métier

#### Validation Modifiée
```java
// Avant: Validation manuelle de l'hôpital
if (hospital == null) {
    Notification.show("Veuillez sélectionner un hôpital", ...);
    return false;
}

// Après: Affectation automatique
if (hospital == null) {
    hospital = getCurrentUserHospital();
    if (hospital == null) {
        Notification.show("Impossible de déterminer l'hôpital de l'utilisateur", ...);
        return false;
    }
}
```

#### Création d'Ordre
```java
Order newOrder = new Order();
newOrder.setHospital(hospital); // Hôpital automatique
newOrder.setDoctor(doctor);
newOrder.setPatient(patient);
```

## Workflow Utilisateur

### 1. Création d'Ordre
```
1. Cliquer "Nouvel Ordre"
2. Sélectionner médecin (obligatoire)
3. Sélectionner patient (obligatoire)
4. Hôpital automatiquement affecté ✓
5. Ajouter des examens (optionnel)
6. Sauvegarder
```

### 2. Modification d'Ordre
```
1. Sélectionner un ordre existant
2. Modifier médecin/patient si nécessaire
3. Hôpital original conservé ✓
4. Ajouter/supprimer des examens
5. Sauvegarder
```

## Avantages

### 1. Expérience Utilisateur
- **Simplification**: Moins de champs à remplir
- **Automatisation**: Pas d'erreur de sélection d'hôpital
- **Logique**: L'hôpital correspond à celui de l'utilisateur

### 2. Cohérence des Données
- **Traçabilité**: Chaque ordre est lié à l'hôpital du créateur
- **Sécurité**: Pas de possibilité d'assigner un mauvais hôpital
- **Consistance**: Tous les ordres d'un utilisateur ont le même hôpital

### 3. Gestion Simplifiée
- **Administration**: Les utilisateurs ne gèrent que leurs hôpitaux
- **Permissions**: Contrôle d'accès basé sur l'hôpital
- **Audit**: Historique clair des créations par hôpital

## Cas d'Usage

### 1. Secrétaire d'Hôpital
```
Utilisateur: Marie Dupont (Hôpital Central)
├── Crée un ordre
├── Hôpital automatiquement: "Hôpital Central"
├── Sélectionne Dr. Martin
├── Sélectionne Patient Jean Durand
└── Sauvegarde
```

### 2. Modification par Autre Utilisateur
```
Utilisateur: Pierre Martin (Hôpital Nord)
├── Modifie l'ordre de Marie
├── Hôpital conservé: "Hôpital Central"
├── Peut modifier médecin/patient
└── Ne peut pas changer l'hôpital
```

## Gestion des Erreurs

### 1. Utilisateur Sans Hôpital
```
Erreur: "Impossible de déterminer l'hôpital de l'utilisateur"
Solution: Configurer l'hôpital dans le profil utilisateur
```

### 2. Session Expirée
```
Erreur: Exception lors de la récupération de l'utilisateur
Solution: Reconnexion requise
```

### 3. Hôpital Inexistant
```
Erreur: L'hôpital de l'utilisateur n'existe plus
Solution: Mettre à jour le profil utilisateur
```

## Impact sur le Code Existant

### 1. Compatibilité
- **Formulaires**: Champ hôpital caché mais toujours présent
- **Validations**: Adaptées pour l'affectation automatique
- **API**: Aucun changement requis

### 2. Migration
- **Ordres existants**: Conservent leur hôpital original
- **Nouveaux ordres**: Hôpital automatique
- **Utilisateurs**: Doivent avoir un hôpital configuré

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

## Bonnes Pratiques

### 1. Validation
- Toujours vérifier que l'hôpital est disponible
- Gérer les cas où l'utilisateur n'a pas d'hôpital
- Fournir des messages d'erreur clairs

### 2. Sécurité
- Ne jamais permettre de modifier l'hôpital manuellement
- Valider que l'utilisateur a les droits sur son hôpital
- Logger les tentatives d'affectation

### 3. Performance
- Mettre en cache l'hôpital de l'utilisateur
- Éviter les requêtes répétées à la base de données
- Gérer correctement les sessions

## Évolutions Futures Possibles

1. **Multi-hôpitaux**: Permettre aux utilisateurs de gérer plusieurs hôpitaux
2. **Validation avancée**: Contrôles plus stricts sur les permissions
3. **Audit complet**: Journalisation détaillée des créations/modifications
4. **Interface admin**: Interface pour gérer les associations utilisateur-hôpital

Cette fonctionnalité simplifie considérablement le workflow de création d'ordres tout en assurant la cohérence et la sécurité des données.
