-- ========================================
-- Vérification de la structure de la table orders
-- ========================================

-- Afficher la structure actuelle de la table orders
\d orders;

-- Afficher les contraintes de la table orders
\dc orders;

-- Afficher les index de la table orders
\di orders;

-- Afficher quelques lignes pour voir les données
SELECT 
    id,
    accession_number,
    study_instance_uid,
    hospital_id,
    doctor_id,
    patient_id,
    created_at,
    updated_at
FROM orders 
ORDER BY created_at DESC 
LIMIT 10;

-- Vérifier s'il y a des valeurs NULL dans hospital_id
SELECT 
    COUNT(*) as total_orders,
    COUNT(hospital_id) as orders_with_hospital,
    COUNT(*) - COUNT(hospital_id) as orders_without_hospital
FROM orders;

-- Afficher les ordres avec hospital_id NULL
SELECT 
    id,
    accession_number,
    study_instance_uid,
    doctor_id,
    patient_id,
    created_at
FROM orders 
WHERE hospital_id IS NULL
ORDER BY created_at DESC;
