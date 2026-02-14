-- ========================================
-- Correction simple des valeurs NULL dans hospital_id
-- ========================================

BEGIN;

-- Mettre à jour les ordres avec hospital_id NULL
-- Utiliser le premier hôpital disponible (ID=1)
UPDATE orders 
SET hospital_id = 1, updated_at = CURRENT_TIMESTAMP
WHERE hospital_id IS NULL;

-- Afficher le résultat
SELECT 
    'Mise à jour effectuée' as status,
    COUNT(*) as total_orders,
    COUNT(hospital_id) as orders_with_hospital,
    COUNT(*) - COUNT(hospital_id) as null_orders_fixed
FROM orders;

COMMIT;
