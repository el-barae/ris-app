@echo off
echo Démarrage de l'application SAHTY avec variables d'environnement...

REM Configuration PostgreSQL
set DB_HOST=localhost
set DB_PORT=5432
set DB_NAME=radiology_db
set DB_USERNAME=postgres
set DB_PASSWORD=1234abcD.

REM Configuration du serveur
set PORT=8080

echo Variables d'environnement définies:
echo DB_HOST=%DB_HOST%
echo DB_PORT=%DB_PORT%
echo DB_NAME=%DB_NAME%
echo DB_USERNAME=%DB_USERNAME%
echo PORT=%PORT%
echo.

echo Lancement de l'application...
mvn spring-boot:run

pause
