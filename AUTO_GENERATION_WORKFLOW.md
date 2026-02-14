# Génération Automatique des Identifiants et Workflow Ordre-Examens

## Overview
Implémentation de la génération automatique des identifiants DICOM et d'un workflow complet pour la création d'ordres avec plusieurs examens, similaire à l'ExamView.

## Génération Automatique des Identifiants

### 1. Utilitaire DicomUidGenerator
**Fichier**: `src/main/java/com/application/util/DicomUidGenerator.java`

#### Méthodes Disponibles:

```java
// Génération Study Instance UID (conforme DICOM)
public static String generateStudyInstanceUID()
// Format: 1.2.276.0.7230010.3.1.2.[random1].1.[timestamp].[random2]

// Génération Accession Number
public static String generateAccessionNumber()
// Format: ACC-[timestamp]-[random]

// Génération Order Accession Number
public static String generateOrderAccessionNumber()
// Format: ORD-[timestamp]-[random]

// Génération SOP Instance UID
public static String generateSOPInstanceUID()
// Format: 1.2.276.0.7230010.3.1.3.[random1].[timestamp].[random2]
```

### 2. Utilisation dans OrderView

#### Génération Automatique:
- **Study Instance UID**: Généré automatiquement pour chaque ordre
- **Accession Number**: Généré automatiquement pour chaque ordre
- **Exam Accession Number**: Généré pour chaque examen
- **Exam SOP Instance UID**: Généré pour chaque examen

#### Suppression des Champs Manuels:
- Plus de champs texte pour accession number et study instance UID
- Les identifiants sont générés en arrière-plan
- Conformité DICOM garantie

## Workflow Ordre-Examens Amélioré

### 1. Interface de Création d'Ordre

#### Section Informations de l'Ordre:
- **Hôpital**: Sélection obligatoire
- **Médecin**: Sélection obligatoire  
- **Patient**: Sélection obligatoire
- **Identifiants**: Générés automatiquement (non visibles)

#### Section Examens:
- **Grille des examens**: Affiche les examens de l'ordre
- **Bouton "Ajouter un examen"**: Ouvre un formulaire simple
- **Actions par examen**: Modifier/Supprimer
- **Gestion en temps réel**: Ajout/suppression immédiat

### 2. Formulaire d'Examen Simplifié

#### Champs Disponibles:
- **Procédure**: Sélection avec modalité
- **Priorité**: NORMAL/URGENT
- **Instructions**: Texte libre

#### Génération Automatique:
- Accession Number unique
- SOP Instance UID unique
- Association automatique à l'ordre

### 3. Workflow Complet

```
1. Créer un ordre
   ├── Sélectionner hôpital (obligatoire)
   ├── Sélectionner médecin (obligatoire)
   ├── Sélectionner patient (obligatoire)
   └── Identifiants générés automatiquement

2. Ajouter des examens
   ├── Cliquer "Ajouter un examen"
   ├── Sélectionner procédure
   ├── Définir priorité
   └── Ajouter instructions

3. Sauvegarder l'ordre
   ├── Validation des champs obligatoires
   ├── Sauvegarde de l'ordre
   ├── Sauvegarde des examens
   └── Association automatique
```

## Avantages de la Nouvelle Approche

### 1. Conformité DICOM
- **UIDs uniques**: Garantis par l'algorithme
- **Format standard**: Respect des normes DICOM
- **Traçabilité**: Identifiants uniques globalement

### 2. Simplicité Utilisateur
- **Moins de champs**: Plus besoin de saisir les identifiants
- **Workflow intuitif**: Similaire à l'ExamView
- **Gestion groupée**: Ordre + examens en une seule opération

### 3. Efficacité
- **Génération instantanée**: Pas d'attente
- **Validation automatique**: Unicité garantie
- **Association transparente**: Liens créés automatiquement

## Structure des Données

### 1. Ordre (Order)
```java
Order order = new Order();
order.setStudyInstanceUID(DicomUidGenerator.generateStudyInstanceUID());
order.setAccessionNumber(DicomUidGenerator.generateOrderAccessionNumber());
order.setHospital(hospital);
order.setDoctor(doctor);
order.setPatient(patient);
```

### 2. Examen (Exam)
```java
Exam exam = new Exam();
exam.setAccessionNumber(DicomUidGenerator.generateAccessionNumber());
exam.setStudyInstanceUID(DicomUidGenerator.generateSOPInstanceUID());
exam.setOrder(order);
exam.setPatient(patient);
exam.setMedecin(doctor);
exam.setProcedure(procedure);
```

### 3. Relations
```
Patient (1) ←→ (N) Order (1) ←→ (N) Exam
   ↓              ↓              ↓
Orders        Exams          Reports
```

## Validation et Contraintes

### 1. Validation Ordre
- **Hôpital**: Obligatoire
- **Médecin**: Obligatoire
- **Patient**: Obligatoire
- **UIDs**: Uniques et valides

### 2. Validation Examen
- **Procédure**: Obligatoire
- **Modalité**: Dérivée de la procédure
- **UIDs**: Uniques et valides
- **Association**: Automatique avec l'ordre

## Cas d'Usage Typique

### 1. Secrétaire Créant un Ordre
```
1. Cliquer "Nouvel Ordre"
2. Sélectionner "Hôpital Central"
3. Sélectionner "Dr. Martin"
4. Sélectionner "Jean Dupont"
5. Ajouter 3 examens:
   ├── CT Thoracique
   ├── IRM Cérébrale
   └── Radio Abdomen
6. Sauvegarder
```

### 2. Résultat
```
Order: ORD-1707898123456-ABC12345
├── Patient: Jean Dupont
├── Médecin: Dr. Martin
├── Hôpital: Hôpital Central
├── Exam 1: ACC-1707898123456-DEF789 (CT Thoracique)
├── Exam 2: ACC-1707898123457-GHI012 (IRM Cérébrale)
└── Exam 3: ACC-1707898123458-JKL345 (Radio Abdomen)
```

## Impact sur le Code Existant

### 1. Compatibilité
- **ExamView**: Route changée vers `/exams-old`
- **OrderView**: Nouvelle interface complète
- **Entités**: Relations mises à jour
- **Repositories**: Méthodes ajoutées

### 2. Migration
- **Ordres existants**: Doivent avoir des patients
- **Examens existants**: Doivent avoir des ordres
- **UIDs**: Nouveau format pour les créations futures

## Bonnes Pratiques

### 1. Génération d'UIDs
- Toujours utiliser DicomUidGenerator
- Ne jamais créer manuellement les UIDs
- Vérifier l'unicité en base de données

### 2. Workflow
- Créer l'ordre d'abord
- Ajouter les examens ensuite
- Sauvegarder le tout en même temps

### 3. Validation
- Valider les champs obligatoires
- Vérifier les relations
- Gérer les erreurs utilisateur

## Évolutions Futures Possibles

1. **Templates d'ordres**: Pré-configurations avec examens
2. **Import DICOM**: Importation depuis d'autres systèmes
3. **Validation avancée**: Contraintes métier complexes
4. **Export DICOM**: Génération de fichiers DICOM
5. **Intégration RIS**: Synchronisation avec d'autres systèmes

Cette implémentation offre une solution complète et conforme aux standards DICOM pour la gestion des ordres et examens médicaux.
