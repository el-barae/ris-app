-- ========================================
-- Initialisation propre de la base de données radiologie
-- ========================================

-- Suppression de la base existante
DROP DATABASE IF EXISTS radiology_db;

-- Création avec encodage UTF-8
CREATE DATABASE radiology_db WITH ENCODING 'UTF8';

-- Connexion
\c radiology_db;

-- Message de confirmation
DO $$
BEGIN
    RAISE NOTICE '✅ Base de données radiology_db recréée avec encodage UTF-8';
    RAISE NOTICE 'Prêt pour l''exécution des autres scripts...';
END $$;
