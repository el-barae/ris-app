-- ========================================
-- Création de la base de données radiologique
-- ========================================

-- Créer la base de données
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

-- Créer l'extension UUID si elle n'existe pas
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Créer l'extension pour les timestamps
CREATE EXTENSION IF NOT EXISTS "btree_gist";
