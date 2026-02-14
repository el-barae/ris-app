-- ========================================
-- Nettoyage simple sans contraintes de clé étrangère
-- ========================================

BEGIN;

-- Désactiver temporairement les contraintes de clé étrangère
SET session_replication_role = replica;

-- 1. Supprimer l'ordre corrompu
DELETE FROM orders WHERE 
    accession_number = 'ORD-1771085214298-509CD691' OR
    study_instance_uid = '1.2.276.0.7230010.3.1.2.493970273.1.1771085214.838227';

-- 2. Supprimer les examens orphelins (ceux dont l'ordre n'existe plus)
DELETE FROM exams WHERE order_id NOT IN (SELECT id FROM orders);

-- 3. Supprimer les procédures orphelines
DELETE FROM procedures WHERE 
    id NOT IN (SELECT procedure_id FROM exams WHERE procedure_id IS NOT NULL)
    AND id NOT IN (SELECT id FROM procedure_catalogs);

-- Réactiver les contraintes
SET session_replication_role = DEFAULT;

-- 4. Afficher le résultat
SELECT 
    'Nettoyage terminé. Ordres restants : ' || COUNT(*) as result
FROM orders;

COMMIT;
