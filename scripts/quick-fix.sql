-- ========================================
-- Correction rapide des problèmes de base de données
-- ========================================

-- 1. Créer la base si elle n'existe pas
SELECT 'CREATE DATABASE radiology_db;' WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'radiology_db') \gexec

-- 2. Se connecter à la base
\c radiology_db

-- 3. Ajouter la colonne hospital_id à exams si elle n'existe pas
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'exams' 
        AND column_name = 'hospital_id'
    ) THEN
        ALTER TABLE exams ADD COLUMN hospital_id BIGINT;
        RAISE NOTICE '✅ Colonne hospital_id ajoutée à la table exams';
    ELSE
        RAISE NOTICE 'ℹ️ La colonne hospital_id existe déjà dans exams';
    END IF;
END $$;

-- 4. Mettre à jour les examens sans hospital_id
UPDATE exams 
SET hospital_id = 1
WHERE hospital_id IS NULL;

-- 5. Afficher un résumé
SELECT 
    'Correction terminée' as status,
    (SELECT COUNT(*) FROM exams) as total_exams,
    (SELECT COUNT(*) FROM exams WHERE hospital_id IS NOT NULL) as exams_with_hospital
FROM dual;

COMMIT;
