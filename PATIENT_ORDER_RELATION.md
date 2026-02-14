# Relation Patient-Order Documentation

## Overview
Une nouvelle relation a été ajoutée entre les entités Patient et Order pour permettre à un patient d'avoir plusieurs ordres médicaux.

## Structure des Relations

### Patient → Order (One-to-Many)
- **Type**: Un patient peut avoir plusieurs ordres
- **Relation**: `@OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)`
- **Base de données**: `patient_id` dans la table `orders`

### Order → Patient (Many-to-One)
- **Type**: Un ordre appartient à un patient
- **Relation**: `@ManyToOne(fetch = FetchType.LAZY)`
- **Contrainte**: `@NotNull(message = "Patient is required")`

## Modifications Apportées

### 1. Entité Patient
```java
@OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
@EqualsAndHashCode.Exclude
private List<Order> orders;
```

### 2. Entité Order
```java
@NotNull(message = "Patient is required")
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "patient_id", nullable = false)
@EqualsAndHashCode.Exclude
private Patient patient;
```

### 3. OrderRepository
Nouvelles méthodes ajoutées:
- `findByPatientId(Long patientId)` - Trouver tous les ordres d'un patient
- `countByPatientId(Long patientId)` - Compter les ordres d'un patient

### 4. OrderView
- **Nouveau filtre**: Filtre par patient dans la section de recherche
- **Nouvelle colonne**: Affichage du patient dans la grille des ordres
- **Formulaire**: Champ patient obligatoire dans la création/modification d'ordre
- **Validation**: Le patient est maintenant obligatoire pour créer un ordre

## Workflow Utilisateur

### 1. Création d'un Ordre
1. Sélectionner un patient (obligatoire)
2. Sélectionner un hôpital (obligatoire)
3. Sélectionner un médecin (obligatoire)
4. Les identifiants sont générés automatiquement

### 2. Consultation des Ordres
- **Par patient**: Filtrer pour voir tous les ordres d'un patient spécifique
- **Par médecin**: Voir les ordres prescrits par un médecin
- **Par hôpital**: Voir les ordres d'un établissement

### 3. Gestion des Examens
- Les examens sont toujours associés à un ordre
- L'ordre détermine le patient pour tous les examens associés

## Schéma de Base de Données

```sql
-- Table patients (existante)
CREATE TABLE patients (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_id VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    -- autres champs...
);

-- Table orders (modifiée)
CREATE TABLE orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    study_instance_uid VARCHAR(255) UNIQUE NOT NULL,
    accession_number VARCHAR(255) UNIQUE NOT NULL,
    hospital_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,  -- NOUVEAU CHAMP
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id),
    FOREIGN KEY (doctor_id) REFERENCES users(id),
    FOREIGN KEY (patient_id) REFERENCES patients(id)  -- NOUVELLE CONTRAINTE
);
```

## Avantages de la Relation

### 1. Traçabilité Améliorée
- **Historique complet**: Tous les ordres d'un patient sont accessibles
- **Suivi médical**: Facile de voir l'évolution des prescriptions
- **Audit trail**: Conservation de l'historique des ordres

### 2. Organisation Logique
- **Structure hiérarchique**: Patient → Order → Exam
- **Cohérence**: Respect du flux médical naturel
- **Facilité**: Un ordre regroupe les examens pour un patient

### 3. Fonctionnalités Étendues
- **Filtrage**: Recherche par patient dans OrderView
- **Statistiques**: Compter les ordres par patient
- **Export**: Générer des rapports par patient

## Impact sur le Code Existant

### 1. Compatibilité
- **ExamView**: Non affectée (route changée vers `/exams-old`)
- **OrderView**: Mise à jour avec nouvelle relation
- **Exam**: La relation Order→Exam reste inchangée

### 2. Migration
- **Ordres existants**: Doivent être associés à des patients
- **Contrainte**: `patient_id` ne peut pas être NULL
- **Validation**: Ajout de la validation du patient

## Cas d'Usage

### 1. Patient avec Plusieurs Ordres
```
Patient: Jean Dupont
├── Order #1: CT Thoracique (15/01/2024)
├── Order #2: IRM Cérébrale (20/01/2024)
└── Order #3: Radio Abdomen (25/01/2024)
```

### 2. Ordre avec Plusieurs Examens
```
Order: CT Thoracique
├── Exam: CT avec injection
├── Exam: CT sans injection
└── Exam: CT avec reconstruction 3D
```

### 3. Workflow Complet
```
Patient → Order → Exam → Report
   ↓        ↓       ↓        ↓
Dossier   Prescription  Imagerie  Interprétation
```

## Bonnes Pratiques

### 1. Validation
- Toujours vérifier que le patient n'est pas null
- Valider l'unicité des ordres par patient
- Gérer les cas de patients anonymes

### 2. Performance
- Utiliser le chargement lazy pour les relations
- Indexer `patient_id` dans la base de données
- Limiter le nombre d'ordres chargés simultanément

### 3. Sécurité
- Contrôler l'accès aux ordres par rôle
- Valider les permissions sur les données patients
- Maintenir l'audit des modifications

## Évolutions Futures Possibles

1. **Templates d'ordres**: Modèles prédéfinis par type de patient
2. **Workflow avancé**: États et transitions complexes
3. **Intégration**: Synchronisation avec d'autres systèmes médicaux
4. **Export**: Génération de rapports par patient
5. **Notifications**: Alertes pour nouveaux ordres

Cette relation Patient-Order améliore considérablement la gestion des dossiers médicaux en offrant une meilleure organisation et traçabilité des prescriptions.
