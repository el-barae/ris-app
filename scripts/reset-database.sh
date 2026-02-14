#!/bin/bash

echo "========================================"
echo "Réinitialisation de la base de données radiology_app"
echo "========================================"
echo

# Vérifier si psql est disponible
if ! command -v psql &> /dev/null; then
    echo "ERREUR: psql n'est pas trouvé dans le PATH."
    echo "Veuillez installer PostgreSQL ou ajouter psql au PATH."
    echo
    echo "Installation sur macOS: brew install postgresql"
    echo "Installation sur Ubuntu: sudo apt-get install postgresql-client"
    exit 1
fi

echo "Connexion à PostgreSQL avec l'utilisateur postgres..."
echo

# Exécuter le script de réinitialisation
psql -U postgres -h localhost -f scripts/99-database-reset.sql

if [ $? -eq 0 ]; then
    echo
    echo "========================================"
    echo "Base de données réinitialisée avec succès!"
    echo "========================================"
else
    echo
    echo "ERREUR: La réinitialisation a échoué."
    echo "Vérifiez les messages d'erreur ci-dessus."
    exit 1
fi

echo
