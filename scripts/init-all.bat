@echo off
setlocal enabledelayedexpansion

REM ========================================
REM Script d'initialisation complète de la base de données (Windows)
REM ========================================

echo.
echo 🚀 Initialisation de la base de données radiologique...
echo.

REM Variables par défaut
set DB_USER=postgres
set DB_HOST=localhost
set DB_PORT=5432
set DB_NAME=radiology_app
set SCRIPT_DIR=%~dp0

REM Demander les informations de connexion
set /p DB_USER="Entrez le nom d'utilisateur PostgreSQL (défaut: postgres): "
if "%DB_USER%"=="" set DB_USER=postgres

set /p DB_HOST="Entrez l'hôte PostgreSQL (défaut: localhost): "
if "%DB_HOST%"=="" set DB_HOST=localhost

set /p DB_PORT="Entrez le port PostgreSQL (défaut: 5432): "
if "%DB_PORT%"=="" set DB_PORT=5432

REM Vérifier si PostgreSQL est accessible
echo.
echo [INFO] Test de connexion à PostgreSQL...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d postgres -c "SELECT 1;" >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Impossible de se connecter à PostgreSQL
    echo.
    echo Vérifiez que:
    echo   - PostgreSQL est en cours d'exécution
    echo   - Les identifiants sont corrects
    echo   - PostgreSQL accepte les connexions
    pause
    exit /b 1
)

echo [SUCCESS] Connexion à PostgreSQL établie

REM Étape 1: Création de la base de données
echo.
echo [INFO] Étape 1/4: Création de la base de données...

REM Vérifier si la base de données existe déjà
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d postgres -c "SELECT 1 FROM pg_database WHERE datname = '%DB_NAME%'" 2>nul | findstr "1" >nul
if not errorlevel 1 (
    echo [WARNING] La base de données '%DB_NAME%' existe déjà
    set /p confirm="Voulez-vous la supprimer et la recréer? (y/N): "
    if /i "!confirm!"=="y" (
        psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d postgres -c "DROP DATABASE IF EXISTS %DB_NAME%;" >nul
        echo [INFO] Base de données existante supprimée
    ) else (
        echo [INFO] Utilisation de la base de données existante
    )
)

psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -f "%SCRIPT_DIR%01-create-database.sql" >nul
if errorlevel 1 (
    echo [ERROR] Erreur lors de la création de la base de données
    pause
    exit /b 1
)
echo [SUCCESS] Base de données créée avec succès

REM Étape 2: Création des tables
echo.
echo [INFO] Étape 2/4: Création des tables...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f "%SCRIPT_DIR%02-create-tables.sql" >nul
if errorlevel 1 (
    echo [ERROR] Erreur lors de la création des tables
    pause
    exit /b 1
)
echo [SUCCESS] Tables créées avec succès

REM Étape 3: Insertion des données initiales
echo.
echo [INFO] Étape 3/4: Insertion des données initiales...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f "%SCRIPT_DIR%03-insert-initial-data.sql" >nul
if errorlevel 1 (
    echo [ERROR] Erreur lors de l'insertion des données initiales
    pause
    exit /b 1
)
echo [SUCCESS] Données initiales insérées avec succès

REM Étape 4: Création de la table Flyway
echo.
echo [INFO] Étape 4/4: Création de la table Flyway...
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -f "%SCRIPT_DIR%04-create-flyway-table.sql" >nul
if errorlevel 1 (
    echo [ERROR] Erreur lors de la création de la table Flyway
    pause
    exit /b 1
)
echo [SUCCESS] Table Flyway créée avec succès

REM Afficher un résumé
echo.
echo 🎉 Initialisation terminée avec succès!
echo.
echo 📊 Résumé de la base de données:
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "
SELECT 
    'modality_types' as table_name, COUNT(*) as count FROM modality_types
UNION ALL SELECT 
    'modalities', COUNT(*) FROM modalities
UNION ALL SELECT 
    'users', COUNT(*) FROM users
UNION ALL SELECT 
    'patients', COUNT(*) FROM patients
UNION ALL SELECT 
    'procedure_catalogs', COUNT(*) FROM procedure_catalogs
UNION ALL SELECT 
    'exams', COUNT(*) FROM exams
UNION ALL SELECT 
    'reports', COUNT(*) FROM reports
ORDER BY table_name;
"

echo.
echo 👤 Utilisateurs créés:
psql -h %DB_HOST% -p %DB_PORT% -U %DB_USER% -d %DB_NAME% -c "SELECT username, role, first_name, last_name FROM users ORDER BY role;"

echo.
echo 🔑 Informations de connexion:
echo   - Base de données: %DB_NAME%
echo   - Hôte: %DB_HOST%
echo   - Port: %DB_PORT%
echo   - Utilisateur: %DB_USER%
echo.
echo 👤 Utilisateurs par défaut (mot de passe: admin123):
echo   - admin (ADMIN)
echo   - dr_dupont (MEDECIN)
echo   - dr_martin (MEDECIN)
echo   - tech1 (TECHNICIEN)
echo   - sec1 (SECRETAIRE)
echo   - radio1 (RADIOLOGUE)
echo.
echo ⚙️  Configuration Spring Boot:
echo   spring.datasource.url=jdbc:postgresql://%DB_HOST%:%DB_PORT%/%DB_NAME%
echo   spring.datasource.username=%DB_USER%
echo   spring.datasource.password=votre_mot_de_passe_postgres
echo.
echo [SUCCESS] Base de données prête à être utilisée!
echo.
pause
