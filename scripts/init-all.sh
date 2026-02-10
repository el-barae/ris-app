#!/bin/bash

# ========================================
# Script d'initialisation complète de la base de données
# ========================================

set -e  # Arrêter le script en cas d'erreur

echo "🚀 Initialisation de la base de données radiologique..."

# Couleurs pour une meilleure lisibilité
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Fonction pour afficher les messages
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Vérifier si PostgreSQL est en cours d'exécution
print_status "Vérification du service PostgreSQL..."
if ! pg_isready -q; then
    print_error "PostgreSQL n'est pas en cours d'exécution"
    print_status "Veuillez démarrer PostgreSQL:"
    echo "  - macOS: brew services start postgresql"
    echo "  - Ubuntu: sudo systemctl start postgresql"
    echo "  - Windows: Démarrer le service PostgreSQL"
    exit 1
fi

print_success "PostgreSQL est en cours d'exécution"

# Demander les informations de connexion si non fournies
if [ -z "$DB_USER" ]; then
    read -p "Entrez le nom d'utilisateur PostgreSQL (défaut: postgres): " DB_USER
    DB_USER=${DB_USER:-postgres}
fi

if [ -z "$DB_HOST" ]; then
    read -p "Entrez l'hôte PostgreSQL (défaut: localhost): " DB_HOST
    DB_HOST=${DB_HOST:-localhost}
fi

if [ -z "$DB_PORT" ]; then
    read -p "Entrez le port PostgreSQL (défaut: 5432): " DB_PORT
    DB_PORT=${DB_PORT:-5432}
fi

# Vérifier la connexion
print_status "Test de connexion à PostgreSQL..."
if ! psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "SELECT 1;" > /dev/null 2>&1; then
    print_error "Impossible de se connecter à PostgreSQL"
    print_status "Vérifiez vos identifiants et que PostgreSQL accepte les connexions"
    exit 1
fi

print_success "Connexion à PostgreSQL établie"

# Variables
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DB_NAME="radiology_app"

# 1. Création de la base de données
print_status "Étape 1/4: Création de la base de données..."
if psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "SELECT 1 FROM pg_database WHERE datname = '$DB_NAME'" | grep -q 1; then
    print_warning "La base de données '$DB_NAME' existe déjà"
    read -p "Voulez-vous la supprimer et la recréer? (y/N): " confirm
    if [[ $confirm =~ ^[Yy]$ ]]; then
        psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "DROP DATABASE IF EXISTS $DB_NAME;"
        print_status "Base de données existante supprimée"
    else
        print_status "Utilisation de la base de données existante"
    fi
fi

psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -f "$SCRIPT_DIR/01-create-database.sql"
print_success "Base de données créée avec succès"

# 2. Création des tables
print_status "Étape 2/5: Création des tables..."
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$SCRIPT_DIR/02-create-tables.sql"
print_success "Tables créées avec succès"

# 3. Insertion des données initiales
print_status "Étape 3/5: Insertion des données initiales..."
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$SCRIPT_DIR/03-insert-initial-data.sql"
print_success "Données initiales insérées avec succès"

# 4. Création de la table Flyway
print_status "Étape 4/5: Création de la table Flyway..."
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$SCRIPT_DIR/04-create-flyway-table.sql"
print_success "Table Flyway créée avec succès"

# 5. Ajout de l'entité Hospital (migration)
print_status "Étape 5/5: Ajout de l'entité Hospital..."
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -f "$SCRIPT_DIR/05-add-hospital-entity.sql"
print_success "Entité Hospital ajoutée avec succès"

# Afficher un résumé
echo ""
echo "🎉 Initialisation terminée avec succès!"
echo ""
echo "📊 Résumé de la base de données:"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "
SELECT 
    'hospitals' as table_name, COUNT(*) as count FROM hospitals
UNION ALL SELECT 
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

echo ""
echo "👤 Utilisateurs créés:"
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" -c "SELECT username, role, first_name, last_name FROM users ORDER BY role;"

echo ""
echo "🔑 Informations de connexion:"
echo "  - Base de données: $DB_NAME"
echo "  - Hôte: $DB_HOST"
echo "  - Port: $DB_PORT"
echo "  - Utilisateur: $DB_USER"
echo ""
echo "👤 Utilisateurs par défaut (mot de passe: admin123):"
echo "  - admin (ADMIN)"
echo "  - dr_dupont (MEDECIN)"
echo "  - dr_martin (MEDECIN)"
echo "  - tech1 (TECHNICIEN)"
echo "  - sec1 (SECRETAIRE)"
echo "  - radio1 (RADIOLOGUE)"
echo ""
echo "⚙️  Configuration Spring Boot:"
echo "  spring.datasource.url=jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME"
echo "  spring.datasource.username=$DB_USER"
echo "  spring.datasource.password=votre_mot_de_passe_postgres"
echo ""
print_success "Base de données prête à être utilisée!"
