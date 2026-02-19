-- Script pour corriger le StudyInstanceUID de l'examen ACC202602141649287809
-- Remplace le StudyInstanceUID incorrect par celui qui fonctionne dans OHIF

UPDATE exams 
SET study_instance_uid = '1.2.276.0.7230010.3.1.2.926497073.1.1771430058.214605'
WHERE accession_number = 'ACC202602141649287809';

-- Vérification
SELECT accession_number, study_instance_uid, status 
FROM exams 
WHERE accession_number = 'ACC202602141649287809';
