-- ========================================
-- Ajout de la colonne hospital_id à la table exams (si nécessaire)
-- ========================================

-- ATTENTION : Cette option ajoute une redondance (hospital_id déjà dans orders)
-- À n'utiliser que si l'application a absolument besoin de cette colonne

BEGIN;

-- Ajouter la colonne hospital_id à la table exams
ALTER TABLE exams ADD COLUMN IF NOT EXISTS hospital_id BIGINT;

-- Créer l'index pour la nouvelle colonne
CREATE INDEX IF NOT EXISTS idx_exams_hospital_id ON exams(hospital_id);

-- Créer le trigger pour maintenir la cohérence
CREATE OR REPLACE FUNCTION sync_exam_hospital_id()
RETURNS TRIGGER AS $$
BEGIN
    -- Mettre à jour hospital_id depuis l'ordre associé
    IF NEW.order_id IS NOT NULL THEN
        NEW.hospital_id = (SELECT hospital_id FROM orders WHERE id = NEW.order_id);
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Créer le trigger
CREATE TRIGGER trg_exams_sync_hospital_id
    BEFORE INSERT OR UPDATE ON exams
    FOR EACH ROW
    EXECUTE FUNCTION sync_exam_hospital_id();

-- Mettre à jour les données existantes
UPDATE exams 
SET hospital_id = (SELECT hospital_id FROM orders WHERE id = exams.order_id)
WHERE hospital_id IS NULL;

COMMIT;
