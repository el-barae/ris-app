-- Script pour mettre à jour la contrainte exams_status_check
-- afin d'autoriser le statut SELECTED

-- Supprimer l'ancienne contrainte
ALTER TABLE exams DROP CONSTRAINT IF EXISTS exams_status_check;

-- Créer la nouvelle contrainte avec tous les statuts possibles
ALTER TABLE exams ADD CONSTRAINT exams_status_check 
CHECK (status IN ('PLANNED', 'SELECTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'));
