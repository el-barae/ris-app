#!/bin/bash

echo "Démarrage de l'application SAHTY avec variables d'environnement..."

# Configuration PostgreSQL
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=radiology_db
export DB_USERNAME=postgres
export DB_PASSWORD=1234abcD.

# Configuration du serveur
export PORT=8080

echo "Variables d'environnement définies:"
echo "DB_HOST=$DB_HOST"
echo "DB_PORT=$DB_PORT"
echo "DB_NAME=$DB_NAME"
echo "DB_USERNAME=$DB_USERNAME"
echo "PORT=$PORT"
echo ""

echo "Lancement de l'application..."
mvn spring-boot:run
