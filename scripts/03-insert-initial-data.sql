-- ========================================
-- Insertion des données initiales
-- ========================================
BEGIN;
-- Types de modalités
INSERT INTO modality_types (code, name, dicom_code, is_active) VALUES
('CT', 'Tomodensitométrie', 'CT', true),
('MRI', 'Imagerie par Résonance Magnétique', 'MR', true),
('RX', 'Radiographie', 'XR', true),
('US', 'Échographie', 'US', true),
('MG', 'Mammographie', 'MG', true),
('RF', 'Radioscopie', 'RF', true),
('PT', 'Tomographie par Émission de Positrons', 'PT', true),
('XA', 'Angiographie', 'XA', true),
('CR', 'Radiographie Numérisée', 'CR', true),
('DX', 'Radiographie Numérique', 'DX', true);

-- Modalités (équipements)
INSERT INTO modalities (aetitle, nom, description, marque, modele, is_active, modality_type_id) VALUES
-- Équipements CT
('CT1', 'CT Siemens Somatom', 'CT 64 tranches', 'Siemens', 'Somatom Definition AS', true, 1),
('CT2', 'CT GE Lightspeed', 'CT 128 tranches', 'GE Healthcare', 'Lightspeed VCT', true, 1),

-- Équipements IRM
('MRI1', 'IRM Siemens Skyra', 'IRM 3.0 Tesla', 'Siemens', 'Magnetom Skyra', true, 2),
('MRI2', 'IRM GE Signa', 'IRM 1.5 Tesla', 'GE Healthcare', 'Signa HDx', true, 2),

-- Équipements Radiographie
('RX1', 'Radiographie Fixe', 'Salle de radiographie conventionnelle', 'Philips', 'DigitalDiagnost', true, 3),
('RX2', 'Radiographie Mobile', 'Radiographie mobile au lit', 'Siemens', 'Mobilett Mira', true, 3),

-- Équipements Échographie
('US1', 'Échographie Voluson', 'Échographie obstétrique', 'GE Healthcare', 'Voluson E10', true, 4),
('US2', 'Échographie Philips', 'Échographie générale', 'Philips', 'Epiq 7', true, 4),

-- Équipements Mammographie
('MG1', 'Mammographie Hologic', 'Mammographie numérique', 'Hologic', 'Selenia Dimensions', true, 5),

-- Équipements Radioscopie
('RF1', 'Radioscopie Siemens', 'Salle de radioscopie', 'Siemens', 'Artis zee', true, 6);

-- Utilisateurs par défaut
-- Mot de passe: admin123 (hashé avec BCrypt)
INSERT INTO users (username, password, email, first_name, last_name, role, is_active) VALUES
('admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'admin@radiology.com', 'Admin', 'System', 'ADMIN', true),
('dr_dupont', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'dupont@radiology.com', 'Jean', 'Dupont', 'MEDECIN', true),
('dr_martin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'martin@radiology.com', 'Marie', 'Martin', 'MEDECIN', true),
('tech1', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'tech1@radiology.com', 'Pierre', 'Technicien', 'TECHNICIEN', true),
('sec1', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'sec1@radiology.com', 'Sophie', 'Secrétaire', 'SECRETAIRE', true),
('radio1', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'radio1@radiology.com', 'Robert', 'Radiologue', 'RADIOLOGUE', true);

-- Procédures cataloguées
INSERT INTO procedure_catalogs (code, name, description, region, contrast_required, contrast_type, preparation_instructions, duration_minutes, is_active, modality_type_id) VALUES
-- Procédures CT
('CT-CHEST', 'CT Thorax', 'Scanner du thorax avec et sans contraste', 'Chest', true, 'Iodé', 'Jeûne 4h avant l''examen', 15, true, 1),
('CT-ABDOMEN', 'CT Abdomen', 'Scanner de l''abdomen avec contraste', 'Abdomen', true, 'Iodé', 'Jeûne 6h avant l''examen', 20, true, 1),
('CT-HEAD', 'CT Crâne', 'Scanner du crâne sans contraste', 'Head', false, null, null, 10, true, 1),
('CT-SPINE', 'CT Rachis', 'Scanner du rachis lombaire', 'Spine', false, null, null, 15, true, 1),

-- Procédures IRM
('MRI-BRAIN', 'IRM Cerveau', 'IRM cérébrale avec et sans contraste', 'Head', true, 'Gadolinium', 'Jeûne 4h avant l''examen', 30, true, 2),
('MRI-KNEE', 'MRI Genou', 'IRM du genou sans contraste', 'Extremity', false, null, null, 25, true, 2),
('MRI-SPINE', 'MRI Rachis', 'IRM du rachis cervical', 'Spine', false, null, null, 30, true, 2),

-- Procédures Radiographie
('RX-CHEST', 'Radio Thorax', 'Radiographie pulmonaire de face et profil', 'Chest', false, null, null, 10, true, 3),
('RX-ABDOMEN', 'RX Abdomen', 'Radiographie abdominale sans préparation', 'Abdomen', false, null, null, 10, true, 3),
('RX-EXTREMITY', 'RX Membre', 'Radiographie de membre (bras/jambe)', 'Extremity', false, null, null, 10, true, 3),

-- Procédures Échographie
('US-ABDOMEN', 'Écho Abdomen', 'Échographie abdominale complète', 'Abdomen', false, null, 'Jeûne 6h avant l''examen', 20, true, 4),
('US-PELVIS', 'Écho Pelvien', 'Échographie pelvienne', 'Pelvis', false, null, 'Vessie pleine', 15, true, 4),
('US-CAROTID', 'Écho Carotides', 'Échographie des artères carotides', 'Neck', false, null, null, 15, true, 4),
('US-OBSTETRIC', 'Écho Obstétricale', 'Échographie obstétricale', 'Pelvis', false, null, 'Vessie pleine', 25, true, 4),

-- Procédures Mammographie
('MG-SCREENING', 'Mammo Dépistage', 'Mammographie de dépistage bilatérale', 'Chest', false, null, null, 15, true, 5),
('MG-DIAGNOSTIC', 'Mammo Diagnostic', 'Mammographie diagnostique unilatérale', 'Chest', false, null, null, 20, true, 5),

-- Procédures Radioscopie
('RF-GI', 'Scopie Digestive', 'Transit œso-gastro-duodénal', 'Abdomen', true, 'Baryum', 'Jeûne 6h avant l''examen', 30, true, 6);

-- Patients exemples
INSERT INTO patients (patient_id, first_name, last_name, date_of_birth, gender, phone, email, address, city, postal_code, cin, nationality) VALUES
('PAT00001', 'Jean', 'Martin', '1980-03-15', 'MALE', '06 12 34 56 78', 'jean.martin@email.com', '123 rue de la République', 'Paris', '75001', 'AB123456', 'Française'),
('PAT00002', 'Marie', 'Dupont', '1975-07-22', 'FEMALE', '06 23 45 67 89', 'marie.dupont@email.com', '45 avenue des Champs-Élysées', 'Paris', '75008', 'CD789012', 'Française'),
('PAT00003', 'Pierre', 'Durand', '1990-11-30', 'MALE', '06 34 56 78 90', 'pierre.durand@email.com', '78 boulevard Haussmann', 'Paris', '75009', 'EF345678', 'Française'),
('PAT00004', 'Sophie', 'Leroy', '1985-05-18', 'FEMALE', '06 45 67 89 01', 'sophie.leroy@email.com', '234 rue du Faubourg Saint-Honoré', 'Paris', '75008', 'GH901234', 'Française'),
('PAT00005', 'Michel', 'Bernard', '1972-09-08', 'MALE', '06 56 78 90 12', 'michel.bernard@email.com', '567 avenue Foch', 'Paris', '75016', 'IJ567890', 'Française');

-- Patients mineurs avec informations parentales
INSERT INTO patients (patient_id, first_name, last_name, date_of_birth, gender, phone, address, city, postal_code, nationality, parent_first_name, parent_last_name, parent_phone, parent_relationship) VALUES
('PAT00006', 'Lucas', 'Petit', '2015-02-14', 'MALE', null, '89 rue de la Paix', 'Paris', '75002', 'Française', 'François', 'Petit', '06 78 90 12 34', 'Père'),
('PAT00007', 'Emma', 'Martin', '2018-06-25', 'FEMALE', null, '123 avenue Victor Hugo', 'Paris', '75016', 'Française', 'Claire', 'Martin', '06 89 01 23 45', 'Mère');

-- Examens exemples
INSERT INTO exams (accession_number, study_instance_uid, scheduled_date_time, status, priority, patient_id, medecin_id, procedure_id, modality_type_id, modality_id) VALUES
('ACC20250130001', '1.2.840.113619.2.55.3.604688237.761.1243134237.654', '2025-01-30 09:00:00', 'SCHEDULED', 'ROUTINE', 1, 2, 1, 1, 1),
('ACC20250130002', '1.2.840.113619.2.55.3.604688237.761.1243134237.655', '2025-01-30 10:30:00', 'SCHEDULED', 'ROUTINE', 2, 3, 5, 2, 3),
('ACC20250130003', '1.2.840.113619.2.55.3.604688237.761.1243134237.656', '2025-01-30 14:00:00', 'SCHEDULED', 'URGENT', 3, 2, 9, 3, 5),
('ACC20250130004', '1.2.840.113619.2.55.3.604688237.761.1243134237.657', '2025-01-30 15:30:00', 'SCHEDULED', 'ROUTINE', 4, 3, 13, 4, 7),
('ACC20250130005', '1.2.840.113619.2.55.3.604688237.761.1243134237.658', '2025-01-31 08:00:00', 'SCHEDULED', 'ROUTINE', 5, 2, 1, 1, 2);

-- Rapports exemples
INSERT INTO reports (exam_id, findings, impression, recommendation, status, radiologist_id) VALUES
(1, 'Examen normal sans anomalie détectée. Parenchyme pulmonaire d''aspect normal. Pas d''épanchement pleural.', 'Examen thoracique normal.', 'Aucun suivi nécessaire.', 'FINAL', 6),
(2, 'Présence d''une discopathie L4-L5 avec protrusion discale modérée. Pas de signe de conflit radiculaire.', 'Discopathie lombaire L4-L5.', 'Kinésithérapie et surveillance clinique.', 'FINAL', 6);

COMMIT;