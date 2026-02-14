-- ========================================
-- Recréation complète de la base de données radiologie (version corrigée)
-- ========================================

-- Suppression de la base de données existante (si elle existe)
DROP DATABASE IF EXISTS radiology_db;

-- Création de la base de données avec le bon tri et encodage
CREATE DATABASE radiology_db 
    WITH ENCODING 'UTF8' 
    LC_COLLATE='French_France.1252';

-- Connexion à la base de données
\c radiology_db;

-- Message de confirmation
DO $$
BEGIN
    RAISE NOTICE '✅ Base de données radiology_db recréée avec succès';
    RAISE NOTICE 'Encodage : UTF-8';
    RAISE NOTICE 'Tri/Collation : French_France.1252 (compatible Windows)';
    RAISE NOTICE 'Prêt pour la création des tables...';
END $$;
