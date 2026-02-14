-- ========================================
-- Diagnostic complet de l'erreur hospital_id dans exams
-- ========================================

BEGIN;

-- 1. Vérifier si la colonne hospital_id existe dans exams
DO $$
DECLARE
    column_exists BOOLEAN;
BEGIN
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'exams' 
        AND column_name = 'hospital_id'
    ) INTO column_exists;
    
    IF column_exists THEN
        RAISE NOTICE '✅ La colonne hospital_id existe déjà dans exams';
    ELSE
        RAISE NOTICE '❌ La colonne hospital_id n''existe PAS dans exams';
    END IF;
END $$;

-- 2. Compter les examens avec hospital_id NULL
SELECT 
    COUNT(*) as exams_without_hospital,
    COUNT(*) FILTER (WHERE hospital_id IS NOT NULL) as exams_with_hospital
FROM exams;

-- 3. Vérifier les ordres liés aux examens sans hôpital
SELECT 
    COUNT(DISTINCT e.order_id) as problematic_orders,
    COUNT(DISTINCT o.id) FILTER (WHERE o.hospital_id IS NULL) as orders_without_hospital
FROM exams e
INNER JOIN orders o ON e.order_id = o.id;

-- 4. Afficher les examens problématiques
SELECT 
    e.id as exam_id,
    e.accession_number,
    e.order_id,
    o.hospital_id as order_hospital_id,
    e.hospital_id as exam_hospital_id
FROM exams e
INNER JOIN orders o ON e.order_id = o.id
WHERE e.hospital_id IS NULL OR o.hospital_id IS NULL
LIMIT 5;

COMMIT;
