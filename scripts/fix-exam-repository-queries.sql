-- ========================================
-- Correction des requêtes ExamRepository problématiques
-- ========================================

-- Le problème vient des requêtes qui essaient d'accéder à o.hospital.id
-- depuis l'entité Exam qui n'a pas de colonne hospital_id
-- L'accès à l'hôpital doit se faire via l'ordre : e.order.hospital.id

-- Requêtes correctes pour remplacer les requêtes problématiques :

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

-- 3. Vérifier la structure actuelle des tables pour diagnostic
\d exams;
\d orders;

-- 4. Afficher les examens qui posent problème (ceux liés à des ordres sans hôpital)
SELECT e.id, e.accession_number, o.id as order_id, o.hospital_id
FROM exams e
INNER JOIN orders o ON e.order_id = o.id
WHERE o.hospital_id IS NULL
LIMIT 10;
