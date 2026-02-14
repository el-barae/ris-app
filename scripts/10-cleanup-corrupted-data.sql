-- ========================================
-- Nettoyage complet des données corrompues
-- ========================================

BEGIN;

-- 1. Supprimer les ordres avec des données invalides
-- Basé sur l'exemple fourni : ORD-1771085214298-509CD691
DELETE FROM orders WHERE 
    accession_number LIKE '%CD691%' OR
    study_instance_uid LIKE '%1.1771085214.838227%' OR
    hospital_id NOT IN (SELECT id FROM hospitals) OR
    doctor_id NOT IN (SELECT id FROM users) OR
    patient_id NOT IN (SELECT id FROM patients) OR
    created_at IS NULL OR
    updated_at IS NULL;

-- 2. Supprimer les examens orphelins (sans ordre valide)
DELETE FROM exams WHERE 
    order_id NOT IN (SELECT id FROM orders) OR
    patient_id NOT IN (SELECT id FROM patients) OR
    medecin_id NOT IN (SELECT id FROM users) OR
    procedure_id NOT IN (SELECT id FROM procedures) OR
    procedure_id NOT IN (SELECT id FROM procedure_catalogs);

-- 3. Supprimer les procédures orphelines (sans examen)
DELETE FROM procedures WHERE 
    id NOT IN (SELECT procedure_id FROM exams) AND
    id NOT IN (SELECT id FROM procedure_catalogs);

-- 4. Nettoyer les rapports orphelins
DELETE FROM reports WHERE 
    exam_id NOT IN (SELECT id FROM exams) OR
    radiologist_id NOT IN (SELECT id FROM users WHERE role = 'RADIOLOGUE');

-- 5. Afficher un résumé du nettoyage
DO $$
DECLARE
    orders_count INTEGER;
    exams_count INTEGER;
    procedures_count INTEGER;
    reports_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO orders_count FROM orders;
    SELECT COUNT(*) INTO exams_count FROM exams;
    SELECT COUNT(*) INTO procedures_count FROM procedures;
    SELECT COUNT(*) INTO reports_count FROM reports;
    
    RAISE NOTICE 'Nettoyage terminé :';
    RAISE NOTICE '  - Ordres : %', orders_count;
    RAISE NOTICE '  - Examens : %', exams_count;
    RAISE NOTICE '  - Procédures : %', procedures_count;
    RAISE NOTICE '  - Rapports : %', reports_count;
END $$;
COMMIT;

-- ========================================
-- Option : Suppression de la colonne hospital_id
-- ========================================

-- Si vous voulez supprimer complètement la gestion hospitalière :

-- Décommentez le bloc suivant pour supprimer la colonne hospital_id :

/*
BEGIN;

-- Supprimer la contrainte étrangère
ALTER TABLE orders DROP CONSTRAINT IF EXISTS fk_orders_hospital;

-- Supprimer la colonne
ALTER TABLE orders DROP COLUMN IF EXISTS hospital_id;

-- Supprimer l'index associé
DROP INDEX IF EXISTS idx_orders_hospital_id;

COMMIT;
*/
