-- ========================================
-- Ajout simple de la colonne hospital_id (UTF-8)
-- ========================================

BEGIN;

-- Ajouter la colonne si elle n'existe pas
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'exams' 
        AND column_name = 'hospital_id'
    ) THEN
        ALTER TABLE exams ADD COLUMN hospital_id BIGINT;
        RAISE NOTICE '✅ Colonne hospital_id ajoutée avec succès';
    ELSE
        RAISE NOTICE 'ℹ️ La colonne hospital_id existe déjà';
    END IF;
END $$;

-- Mettre à jour les données existantes
UPDATE exams 
SET hospital_id = (SELECT o.hospital_id FROM orders o WHERE o.id = exams.order_id)
WHERE hospital_id IS NULL;

RAISE NOTICE 'Mise à jour terminée. % examens mis à jour', 
    (SELECT COUNT(*) FROM exams WHERE hospital_id IS NULL);

COMMIT;
