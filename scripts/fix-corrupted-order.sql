-- ========================================
-- Correction immédiate de l'ordre corrompu
-- ========================================

-- Supprimer l'ordre spécifique avec des données invalides
DELETE FROM orders WHERE 
    accession_number = 'ORD-1771085214298-509CD691' OR
    study_instance_uid = '1.2.276.0.7230010.3.1.2.493970273.1.1771085214.838227';

-- Vérifier le résultat
SELECT 
    'Ordres supprimés : ' || COUNT(*) as result
FROM orders 
WHERE 
    accession_number = 'ORD-1771085214298-509CD691' OR
    study_instance_uid = '1.2.276.0.7230010.3.1.2.493970273.1.1771085214.838227';
