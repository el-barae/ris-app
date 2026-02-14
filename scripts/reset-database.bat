@echo off
echo ========================================
echo Réinitialisation de la base de données radiology_app
echo ========================================
echo.

REM Vérifier si psql est disponible
where psql >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERREUR: psql n'est pas trouvé dans le PATH.
    echo Veuillez installer PostgreSQL ou ajouter psql au PATH.
    echo.
    echo Emplacement typique de psql:
    echo C:\Program Files\PostgreSQL\{version}\bin
    pause
    exit /b 1
)

echo Connexion à PostgreSQL avec l'utilisateur postgres...
echo.

REM Exécuter le script de réinitialisation
psql -U postgres -h localhost -f scripts/99-database-reset.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo Base de données réinitialisée avec succès!
    echo ========================================
) else (
    echo.
    echo ERREUR: La réinitialisation a échoué.
    echo Vérifiez les messages d'erreur ci-dessus.
)

echo.
pause
