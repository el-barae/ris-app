# Scripts d'initialisation PostgreSQL pour l'application radiologique

## 📋 Description

Ce dossier contient les scripts SQL nécessaires pour créer une base de données PostgreSQL complète pour l'application de gestion radiologique.

## 🗂️ Structure des fichiers

| Fichier | Description |
|---------|-------------|
| `00-drop-database.sql` | Suppression complète de la base de données |
| `01-create-database.sql` | Création de la base de données et extensions |
| `02-create-tables.sql` | Création de toutes les tables avec contraintes et index |
| `03-insert-initial-data.sql` | Insertion des données initiales (utilisateurs, modalités, procédures, etc.) |
| `04-create-flyway-table.sql` | Création de la table Flyway pour le versioning |
| `99-database-reset.sql` | Script complet de réinitialisation (tout-en-un) |
| `init-all.sh` | Script d'exécution automatique (Linux/macOS) |
| `init-all.bat` | Script d'exécution automatique (Windows) |
| `reset-database.sh` | Script de réinitialisation rapide (Linux/macOS) |
| `reset-database.bat` | Script de réinitialisation rapide (Windows) |

## 🚀 Installation rapide

### Prérequis

- PostgreSQL 12+ installé
- Utilisateur PostgreSQL avec droits de création de base de données

### Installation automatique

#### Option 1: Réinitialisation complète recommandée (après modifications des entities)

**Windows:**
```bash
# Réinitialisation complète et rapide
reset-database.bat
```

**Linux/macOS:**
```bash
# Réinitialisation complète et rapide
./reset-database.sh
```

#### Option 2: Installation depuis zéro

**Windows:**
```bash
# Exécuter le script batch
init-all.bat
```

**Linux/macOS:**
```bash
# Rendre le script exécutable
chmod +x init-all.sh

# Exécuter le script
./init-all.sh
```

#### Option 3: Manuellement

**Windows:**
```bash
# Réinitialisation tout-en-un
psql -U postgres -f 99-database-reset.sql
```

**Linux/macOS:**
```bash
# Réinitialisation tout-en-un
psql -U postgres -f 99-database-reset.sql
```

### Installation manuelle

#### 1. Créer la base de données:
```bash
psql -U postgres -f 01-create-database.sql
```

#### 2. Créer les tables:
```bash
psql -U postgres -d radiology_app -f 02-create-tables.sql
```

#### 3. Insérer les données initiales:
```bash
psql -U postgres -d radiology_app -f 03-insert-initial-data.sql
```

#### 4. Créer la table Flyway:
```bash
psql -U postgres -d radiology_app -f 04-create-flyway-table.sql
```

## 👤 Utilisateurs par défaut

| Username | Rôle | Mot de passe | Description |
|----------|------|--------------|-------------|
| `admin` | ADMIN | `admin123` | Administrateur système |
| `dr_dupont` | MEDECIN | `admin123` | Médecin prescripteur |
| `dr_martin` | MEDECIN | `admin123` | Médecin prescripteur |
| `tech1` | TECHNICIEN | `admin123` | Technicien radiologie |
| `sec1` | SECRETAIRE | `admin123` | Secrétaire médicale |
| `radio1` | RADIOLOGUE | `admin123` | Radiologue |

## 🏥 Équipements (Modalités)

### Équipements CT:
- `CT1`: CT Siemens Somatom (64 tranches)
- `CT2`: CT GE Lightspeed (128 tranches)

### Équipements IRM:
- `MRI1`: IRM Siemens Skyra (3.0 Tesla)
- `MRI2`: IRM GE Signa (1.5 Tesla)

### Équipements Radiographie:
- `RX1`: Radiographie Fixe Philips
- `RX2`: Radiographie Mobile Siemens

### Autres équipements:
- `US1/US2`: Échographies
- `MG1`: Mammographie
- `RF1`: Radioscopie

## 📊 Données initiales

Le script d'initialisation crée:

- ✅ **10 types de modalités** (CT, MRI, RX, US, MG, etc.)
- ✅ **10 équipements** répartis par type
- ✅ **6 utilisateurs** avec différents rôles
- ✅ **16 procédures** cataloguées
- ✅ **7 patients** (dont 2 mineurs avec infos parentales)
- ✅ **7 examens** (5 PLANNED pour le worklist, 2 SELECTED pour la worklist active)
- ✅ **2 rapports** d'exemples

## 🔧 Configuration Spring Boot

Pour utiliser cette base de données avec votre application Spring Boot, configurez votre `application.properties`:

```properties
# Configuration PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/radiology_app
spring.datasource.username=postgres
spring.datasource.password=votre_mot_de_passe
spring.datasource.driver-class-name=org.postgresql.Driver

# Configuration Flyway
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
spring.flyway.locations=classpath:db/migration
```

## 🔄 Migration avec Flyway

Les scripts de migration existants dans `src/main/resources/db/migration/` sont compatibles avec cette structure:

- `V1__Create_initial_tables.sql`
- `V2__update_gender_other_values.sql`
- `V3__add_procedure_contrast_fields.sql`
- `V4__add_patient_nationality.sql`
- `V5__update_exam_modality_structure.sql`
- `V6__add_patient_parent_info.sql`
- `V7__add_patient_passport.sql`

## 🛠️ Maintenance

### Sauvegarder la base de données:
```bash
pg_dump -U postgres radiology_app > backup_$(date +%Y%m%d_%H%M%S).sql
```

### Restaurer la base de données:
```bash
psql -U postgres radiology_app < backup_20250130_120000.sql
```

### Réinitialiser complètement:
```bash
# Supprimer la base de données
dropdb -U postgres radiology_app

# Recréer depuis zéro
./init-all.sh
```

## 🔐 Sécurité

- **Changez les mots de passe par défaut** après la première connexion
- **Utilisez des mots de passe forts** pour les utilisateurs PostgreSQL
- **Configurez les droits d'accès** selon vos besoins
- **Activez les logs PostgreSQL** pour l'audit

## 📝 Notes importantes

1. **Encodage UTF-8**: La base utilise l'encodage UTF-8 pour supporter les caractères accentués français
2. **Timestamps automatiques**: Les colonnes `created_at` et `updated_at` sont gérées automatiquement
3. **Contraintes d'intégrité**: Toutes les clés étrangères sont définies avec `ON DELETE RESTRICT`
4. **Index optimisés**: Les index sont créés pour optimiser les performances des requêtes courantes

## 🆘 Dépannage

### Erreur "database already exists":
```bash
dropdb -U postgres radiology_app
```

### Erreur "permission denied":
```bash
# Vérifiez que l'utilisateur PostgreSQL a les droits nécessaires
psql -U postgres -c "\du"
```

### Erreur "connection refused":
```bash
# Vérifiez que PostgreSQL est en cours d'exécution
pg_ctl status
```

## 📞 Support

Pour toute question sur l'installation ou l'utilisation de ces scripts, consultez la documentation de l'application ou contactez l'équipe de développement.
