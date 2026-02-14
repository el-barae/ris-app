# OrderView Documentation

## Overview
La `OrderView` remplace l'ancienne `ExamView` pour fournir une interface de gestion des ordres médicaux avec leurs examens associés. Cette vue permet de créer, modifier et supprimer des ordres, ainsi que de gérer les examens au sein de chaque ordre.

## Fonctionnalités

### 1. Gestion des Ordres
- **Création**: Nouvel ordre avec accession number et Study Instance UID
- **Modification**: Mise à jour des informations de l'ordre
- **Suppression**: Suppression d'un ordre et de tous ses examens associés
- **Consultation**: Affichage détaillé d'un ordre avec ses examens

### 2. Gestion des Examens
- **Ajout**: Ajout d'examens à un ordre existant
- **Association**: Lien automatique entre examen et ordre
- **Visualisation**: Affichage des examens dans le détail de l'ordre

### 3. Filtrage et Recherche
- **Recherche textuelle**: Par accession number, Study Instance UID, hôpital, médecin
- **Filtre hôpital**: Sélection par établissement
- **Filtre médecin**: Sélection par praticien
- **Filtre date**: Filtrage par date de création

## Interface Utilisateur

### Header
- **Titre**: "Gestion des Ordres"
- **Badge compteur**: Nombre d'ordres affichés
- **Bouton "Nouvel Ordre"**: Création d'un nouvel ordre

### Section Filtres
- **Champ recherche**: Recherche multi-critères
- **ComboBox Hôpital**: Filtre par établissement
- **ComboBox Médecin**: Filtre par praticien
- **DatePicker**: Filtre par date
- **Bouton Réinitialiser**: Remise à zéro des filtres

### Grille des Ordres
Colonnes affichées:
- **N° Accession**: Identifiant unique de l'ordre
- **Study Instance UID**: UID DICOM de l'étude
- **Hôpital**: Établissement de rattachement
- **Médecin**: Praticien prescripteur
- **Nb. Examens**: Nombre d'examens dans l'ordre
- **Date création**: Timestamp de création
- **Actions**: Voir, Ajouter examen, Modifier, Supprimer

### Formulaires

#### Formulaire Ordre
Champs:
- **N° Accession**: Généré automatiquement si vide
- **Study Instance UID**: UID unique pour l'étude DICOM
- **Hôpital**: Sélection obligatoire
- **Médecin**: Sélection obligatoire

#### Formulaire Examen
Champs:
- **Informations ordre**: Affichage non modifiable
- **Patient**: Sélection obligatoire
- **Procédure**: Sélection avec modalité
- **Instructions**: Texte libre

### Dialogue Détails Ordre
- **Informations ordre**: Accession, UID, hôpital, médecin, date, nombre d'examens
- **Grille examens**: Liste des examens avec statuts
- **Actions**: Fermeture

## Workflow Utilisateur

### 1. Création d'un Ordre
1. Cliquer sur "Nouvel Ordre"
2. Remplir les champs obligatoires (hôpital, médecin)
3. Les identifiants sont générés automatiquement si non fournis
4. Sauvegarder

### 2. Ajout d'Examens
1. Dans la grille, cliquer sur l'icône "+" pour l'ordre souhaité
2. Sélectionner patient et procédure
3. Ajouter des instructions si nécessaire
4. Sauvegarder

### 3. Consultation
1. Cliquer sur l'icône "œil" pour voir les détails
2. Visualiser les informations de l'ordre
3. Consulter la grille des examens associés

### 4. Modification
1. Cliquer sur l'icône "éditer"
2. Modifier les champs nécessaires
3. Sauvegarder

### 5. Suppression
1. Cliquer sur l'icône "corbeille"
2. Confirmer la suppression
3. L'ordre et tous ses examens sont supprimés

## Routes et Accès

- **Route principale**: `/secretaire`
- **Route alias**: `/orders`
- **Rôles autorisés**: ADMIN, MEDECIN, SECRETAIRE, RADIOLOGUE
- **Layout**: MainLayout

## Validation et Contraintes

### Ordre
- **Hôpital**: Obligatoire
- **Médecin**: Obligatoire
- **Accession Number**: Unique
- **Study Instance UID**: Unique

### Examen
- **Patient**: Obligatoire
- **Procédure**: Obligatoire
- **Ordre**: Automatiquement associé

## Intégration Technique

### Dépendances
- `OrderRepository`: Accès données ordres
- `ExamRepository`: Accès données examens
- `OrderService`: Logique métier
- Autres repositories pour les entités associées

### Relations Gérées
- **Order ↔ Hospital**: Many-to-One
- **Order ↔ User (Doctor)**: Many-to-One
- **Order ↔ Exam**: One-to-Many
- **Exam ↔ Order**: Many-to-One (bidirectionnelle)

### Gestion des Erreurs
- Notifications utilisateur pour les erreurs de validation
- Messages d'erreur clairs et spécifiques
- Confirmation pour les actions destructives

## Avantages par rapport à l'ExamView

1. **Structure hiérarchique**: Les examens sont organisés par ordre
2. **Traçabilité**: Meilleure suivi des prescriptions
3. **Gestion simplifiée**: Interface plus intuitive pour la réception
4. **Flexibilité**: Un ordre peut contenir plusieurs examens
5. **Cohérence**: Respect du modèle DICOM (Study → Series)

## Évolutions Possibles

1. **Import/Export**: Importation depuis systèmes externes
2. **Templates**: Modèles d'ordres prédéfinis
3. **Validation DICOM**: Vérification de conformité
4. **Intégration RIS**: Synchronisation avec d'autres systèmes
5. **Workflow avancé**: États et transitions complexes
