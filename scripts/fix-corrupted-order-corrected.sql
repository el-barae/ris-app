-- ========================================
-- Correction de l'ordre corrompu avec gestion des dépendances
-- ========================================

BEGIN;

-- 1. D'abord supprimer les examens liés à l'ordre corrompu
-- On cherche l'ID de l'ordre problématique d'abord
DO $$
DECLARE
    order_id_to_delete BIGINT;
BEGIN
    SELECT id INTO order_id_to_delete FROM orders 
    WHERE accession_number = 'ORD-1771085214298-509CD691' 
       OR study_instance_uid = '1.2.276.0.7230010.3.1.2.493970273.1.1771085214.838227'
    LIMIT 1;
    
    IF order_id_to_delete IS NOT NULL THEN
        -- Supprimer d'abord les examens liés
        DELETE FROM exams WHERE order_id = order_id_to_delete;
        
        -- Puis supprimer l'ordre
        DELETE FROM orders WHERE id = order_id_to_delete;
        
        RAISE NOTICE 'Ordre % et ses examens ont été supprimés avec succès', order_id_to_delete;
    ELSE
        RAISE NOTICE 'Aucun ordre corrompu trouvé à supprimer';
    END IF;
END $$;

-- 2. Nettoyer les procédures orphelines (celles qui ne sont plus utilisées)
DELETE FROM procedures WHERE 
    id NOT IN (SELECT procedure_id FROM exams WHERE procedure_id IS NOT NULL)
    AND id NOT IN (SELECT id FROM procedure_catalogs);

-- 3. Afficher un résumé des opérations
DO $$
DECLARE
    orders_count INTEGER;
    exams_count INTEGER;
    procedures_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO orders_count FROM orders;
    SELECT COUNT(*) INTO exams_count FROM exams;
    SELECT COUNT(*) INTO procedures_count FROM procedures;
    
    RAISE NOTICE '=== RÉSUMÉ APRÈS NETTOYAGE ===';
    RAISE NOTICE 'Ordres : %', orders_count;
    RAISE NOTICE 'Examens : %', exams_count;
    RAISE NOTICE 'Procédures : %', procedures_count;
    RAISE NOTICE '===================================';
END $$;

COMMIT;
