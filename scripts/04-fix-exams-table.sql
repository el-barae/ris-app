-- Correction de la table exams pour rendre patient_id et medecin_id NULLables
-- car ces informations sont maintenant dans l'ordre

BEGIN;

-- Rendre les colonnes patient_id et medecin_id NULLables
ALTER TABLE exams ALTER COLUMN patient_id DROP NOT NULL;
ALTER TABLE exams ALTER COLUMN medecin_id DROP NOT NULL;

-- Mettre à NULL les valeurs existantes si nécessaire (optionnel)
-- UPDATE exams SET patient_id = NULL, medecin_id = NULL WHERE patient_id IS NOT NULL;

COMMIT;
