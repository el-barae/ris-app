-- ========================================
-- Correction des requêtes ExamRepository problématiques (UTF-8)
-- ========================================

-- Requêtes correctes pour remplacer les requêtes problématiques dans ExamRepository.java

-- 1. Pour trouver les examens par hôpital (correct)
SELECT e FROM exams e
INNER JOIN orders o ON e.order_id = o.id
INNER JOIN patients p ON e.patient_id = p.id
INNER JOIN users u ON e.medecin_id = u.id
LEFT JOIN procedures proc ON e.procedure_id = proc.id
LEFT JOIN procedure_catalogs pc ON proc.procedure_catalog_id = pc.id
WHERE o.hospital_id = :hospitalId AND e.status = :status;

-- 2. Alternative plus simple si on a juste besoin des examens par hôpital
SELECT e.* FROM exams e
INNER JOIN orders o ON e.order_id = o.id
WHERE o.hospital_id = :hospitalId;

-- 3. Pour trouver les examens par statut et hôpital
SELECT e FROM exams e
INNER JOIN orders o ON e.order_id = o.id
WHERE e.status = :status AND o.hospital.id = :hospitalId;

-- 4. Vérifier la structure actuelle des tables pour diagnostic
SELECT 
    column_name, 
    data_type, 
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_name = 'exams' 
    AND column_name IN ('hospital_id', 'order_id')
ORDER BY column_name;
