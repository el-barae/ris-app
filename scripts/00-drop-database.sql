-- ========================================
-- Suppression complète de la base de données
-- ========================================

-- Supprimer la base de données si elle existe
DROP DATABASE IF EXISTS radiology_app;

-- Créer la base de données vide
CREATE DATABASE radiology_app 
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    LC_COLLATE = 'fr_FR.UTF-8'
    LC_CTYPE = 'fr_FR.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

-- Se connecter à la base de données
\c radiology_app;
