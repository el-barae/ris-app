-- Correction de la foreign key dans exams pour pointer vers procedures au lieu de procedure_catalogs
BEGIN;

-- Supprimer la contrainte existante si elle existe
ALTER TABLE exams DROP CONSTRAINT IF EXISTS exams_procedure_id_fkey;

-- Ajouter la bonne contrainte vers procedures
ALTER TABLE exams 
    ADD CONSTRAINT exams_procedure_id_fkey 
    FOREIGN KEY (procedure_id) REFERENCES procedures(id);

COMMIT;
