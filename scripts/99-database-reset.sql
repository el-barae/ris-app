-- ========================================
-- Script complet de réinitialisation de la base de données
-- Usage: psql -U postgres -f 99-database-reset.sql
-- ========================================

-- Étape 1: Suppression et recréation de la base de données
\echo "Suppression de la base de données existante..."
DROP DATABASE IF EXISTS radiology_db;

\echo "Création de la base de données radiology_db..."
CREATE DATABASE radiology_db
    WITH 
    OWNER = postgres
    ENCODING = 'UTF8'
    TEMPLATE = template0
    LC_COLLATE = 'fr_FR.UTF-8'
    LC_CTYPE = 'fr_FR.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;

-- Se connecter à la base de données
\c radiology_db;

\echo "Création des extensions..."
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "btree_gist";

-- Étape 2: Création des tables
\echo "Création des tables..."

BEGIN;

-- Table des types de modalités
CREATE TABLE modality_types (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(10) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    dicom_code VARCHAR(16),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des modalités (équipements)
CREATE TABLE modalities (
    id BIGSERIAL PRIMARY KEY,
    aetitle VARCHAR(16) NOT NULL UNIQUE,
    nom VARCHAR(100) NOT NULL,
    description TEXT,
    marque VARCHAR(50),
    modele VARCHAR(50),
    is_active BOOLEAN DEFAULT true,
    modality_type_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (modality_type_id) REFERENCES modality_types(id)
);

-- Table des utilisateurs
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des patients
CREATE TABLE patients (
    id BIGSERIAL PRIMARY KEY,
    patient_id VARCHAR(20) UNIQUE,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    date_of_birth DATE,
    gender VARCHAR(10),
    phone VARCHAR(20),
    email VARCHAR(100),
    address TEXT,
    city VARCHAR(50),
    postal_code VARCHAR(20),
    cin VARCHAR(20),
    passport_number VARCHAR(50),
    nationality VARCHAR(100),
    parent_first_name VARCHAR(50),
    parent_last_name VARCHAR(50),
    parent_phone VARCHAR(20),
    parent_relationship VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des catalogues de procédures
CREATE TABLE procedure_catalogs (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    region VARCHAR(50),
    contrast_required BOOLEAN DEFAULT false,
    contrast_type VARCHAR(50),
    preparation_instructions TEXT,
    duration_minutes INTEGER DEFAULT 30,
    is_active BOOLEAN DEFAULT true,
    modality_type_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (modality_type_id) REFERENCES modality_types(id)
);

-- Table des examens
CREATE TABLE exams (
    id BIGSERIAL PRIMARY KEY,
    accession_number VARCHAR(50) NOT NULL UNIQUE,
    study_instance_uid VARCHAR(64) UNIQUE,
    scheduled_date_time TIMESTAMP,
    performed_date_time TIMESTAMP,
    status VARCHAR(20) DEFAULT 'CREATED',
    priority VARCHAR(20) DEFAULT 'NORMAL',
    additional_instructions TEXT,
    patient_id BIGINT NOT NULL,
    medecin_id BIGINT NOT NULL,
    procedure_id BIGINT,
    modality_type_id BIGINT NOT NULL,
    modality_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    FOREIGN KEY (medecin_id) REFERENCES users(id),
    FOREIGN KEY (procedure_id) REFERENCES procedure_catalogs(id),
    FOREIGN KEY (modality_type_id) REFERENCES modality_types(id),
    FOREIGN KEY (modality_id) REFERENCES modalities(id)
);

-- Table des rapports
CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    exam_id BIGINT NOT NULL UNIQUE,
    findings TEXT,
    impression TEXT,
    recommendation TEXT,
    status VARCHAR(20) DEFAULT 'DRAFT',
    radiologist_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (exam_id) REFERENCES exams(id),
    FOREIGN KEY (radiologist_id) REFERENCES users(id)
);

COMMIT;

-- Étape 3: Création des index
\echo "Création des index..."

BEGIN;

CREATE INDEX idx_patients_patient_id ON patients(patient_id);
CREATE INDEX idx_patients_last_name ON patients(last_name);
CREATE INDEX idx_patients_first_name ON patients(first_name);
CREATE INDEX idx_patients_dob ON patients(date_of_birth);
CREATE INDEX idx_patients_cin ON patients(cin);
CREATE INDEX idx_patients_passport ON patients(passport_number);

CREATE INDEX idx_exams_accession_number ON exams(accession_number);
CREATE INDEX idx_exams_study_uid ON exams(study_instance_uid);
CREATE INDEX idx_exams_patient_id ON exams(patient_id);
CREATE INDEX idx_exams_medecin_id ON exams(medecin_id);
CREATE INDEX idx_exams_status ON exams(status);
CREATE INDEX idx_exams_scheduled_date ON exams(scheduled_date_time);
CREATE INDEX idx_exams_modality_type ON exams(modality_type_id);
CREATE INDEX idx_exams_modality ON exams(modality_id);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

CREATE INDEX idx_procedures_code ON procedure_catalogs(code);
CREATE INDEX idx_procedures_modality_type ON procedure_catalogs(modality_type_id);
CREATE INDEX idx_procedures_region ON procedure_catalogs(region);

CREATE INDEX idx_modalities_aetitle ON modalities(aetitle);
CREATE INDEX idx_modalities_modality_type ON modalities(modality_type_id);

CREATE INDEX idx_modality_types_code ON modality_types(code);

COMMIT;

-- Étape 4: Création des triggers pour les timestamps
\echo "Création des triggers..."

BEGIN;

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Appliquer le trigger à toutes les tables
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_patients_updated_at BEFORE UPDATE ON patients 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_exams_updated_at BEFORE UPDATE ON exams 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_reports_updated_at BEFORE UPDATE ON reports 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_procedures_updated_at BEFORE UPDATE ON procedure_catalogs 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_modalities_updated_at BEFORE UPDATE ON modalities 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_modality_types_updated_at BEFORE UPDATE ON modality_types 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMIT;

-- Étape 5: Insertion des données initiales
\echo "Insertion des données initiales..."

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
('admin', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'admin@radiology.com', 'Admin', 'System', 'ADMIN', true),
('dr_dupont', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'dupont@radiology.com', 'Jean', 'Dupont', 'MEDECIN', true),
('dr_martin', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'martin@radiology.com', 'Marie', 'Martin', 'MEDECIN', true),
('tech1', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'tech1@radiology.com', 'Pierre', 'Technicien', 'TECHNICIEN', true),
('sec1', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'sec1@radiology.com', 'Sophie', 'Secrétaire', 'SECRETAIRE', true),
('radio1', '$2a$10$EblZqNptyYvcLm/VwDCVAuBjzZOI7khzdyGPBr08PpIi0na624b8.', 'radio1@radiology.com', 'Robert', 'Radiologue', 'RADIOLOGUE', true);

-- Procédures cataloguées
INSERT INTO procedure_catalogs (code, name, description, region, contrast_required, contrast_type, preparation_instructions, duration_minutes, is_active, modality_type_id) VALUES
-- Procédures CT
('CT-CHEST', 'CT Thorax', 'Scanner du thorax avec et sans contraste', 'Chest', true, 'Iodé', 'Jeûne 4h avant l''examen', 15, true, 1),
('CT-ABDOMEN', 'CT Abdomen', 'Scanner de l''abdomen avec contraste', 'Abdomen', true, 'Iodé', 'Jeûne 6h avant l''examen', 20, true, 1),
('CT-HEAD', 'CT Crâne', 'Scanner du crâne sans contraste', 'Head', false, null, null, 10, true, 1),
('CT-SPINE', 'CT Rachis', 'Scanner du rachis lombaire', 'Spine', false, null, null, 15, true, 1),

-- Procédures IRM
('MRI-BRAIN', 'IRM Cerveau', 'IRM cérébrale avec et sans contraste', 'Head', true, 'Gadolinium', 'Jeûne 4h avant l''examen', 30, true, 2),
('MRI-KNEE', 'IRM Genou', 'IRM du genou sans contraste', 'Extremity', false, null, null, 25, true, 2),
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

-- Orders exemples (nécessaires pour la nouvelle architecture)
INSERT INTO orders (study_instance_uid, accession_number, hospital_id, doctor_id, patient_id, created_at) VALUES
('1.2.840.113619.2.55.3.604688237.761.1243134237.654', 'ORD-20250214001', 1, 2, 1, CURRENT_TIMESTAMP),
('1.2.840.113619.2.55.3.604688237.761.1243134237.655', 'ORD-20250214002', 1, 3, 2, CURRENT_TIMESTAMP),
('1.2.840.113619.2.55.3.604688237.761.1243134237.656', 'ORD-20250214003', 1, 2, 3, CURRENT_TIMESTAMP),
('1.2.840.113619.2.55.3.604688237.761.1243134237.657', 'ORD-20250214004', 1, 3, 4, CURRENT_TIMESTAMP),
('1.2.840.113619.2.55.3.604688237.761.1243134237.658', 'ORD-20250214005', 1, 2, 5, CURRENT_TIMESTAMP),
('1.2.840.113619.2.55.3.604688237.761.1243134237.659', 'ORD-20250214006', 1, 2, 1, CURRENT_TIMESTAMP),
('1.2.840.113619.2.55.3.604688237.761.1243134237.660', 'ORD-20250214007', 1, 3, 3, CURRENT_TIMESTAMP);

-- Procedures exemples (instances spécifiques)
INSERT INTO procedures (name, procedure_code, modality_type_id, procedure_catalog_id, contrast_required, is_active, created_at) VALUES
('CT Thorax Standard', 'CT-THORAX', 1, 1, false, true, CURRENT_TIMESTAMP),
('CT Abdomen Pelvis', 'CT-ABDOMEN', 2, 2, true, true, CURRENT_TIMESTAMP),
('IRM Cérébrale', 'MR-BRAIN', 3, 3, false, true, CURRENT_TIMESTAMP),
('RX Thorax PA', 'RX-CHEST', 4, 4, false, true, CURRENT_TIMESTAMP),
('Échographie Abdomen', 'US-ABDOMEN', 5, 5, false, true, CURRENT_TIMESTAMP),
('CT Thorax avec contraste', 'CT-THORAX-CONTRAST', 1, 1, true, true, CURRENT_TIMESTAMP),
('IRM Rachis Lombaire', 'MR-SPINE', 3, 3, false, true, CURRENT_TIMESTAMP);

-- Examens exemples avec la nouvelle architecture
INSERT INTO exams (accession_number, study_instance_uid, scheduled_date_time, status, priority, order_id, modality_type_id, modality_id, procedure_id) VALUES
('ACC20250214001', '1.2.840.113619.2.55.3.604688237.761.1243134237.654', '2025-02-14 09:00:00', 'PLANNED', 'NORMAL', 1, 1, 1, 1),
('ACC20250214002', '1.2.840.113619.2.55.3.604688237.761.1243134237.655', '2025-02-14 10:30:00', 'PLANNED', 'NORMAL', 2, 2, 3, 2),
('ACC20250214003', '1.2.840.113619.2.55.3.604688237.761.1243134237.656', '2025-02-14 14:00:00', 'PLANNED', 'URGENT', 3, 3, 5, 3),
('ACC20250214004', '1.2.840.113619.2.55.3.604688237.761.1243134237.657', '2025-02-14 15:30:00', 'PLANNED', 'NORMAL', 4, 4, 7, 4),
('ACC20250214005', '1.2.840.113619.2.55.3.604688237.761.1243134237.658', '2025-02-15 08:00:00', 'PLANNED', 'NORMAL', 5, 1, 2, 1),
('ACC20250214006', '1.2.840.113619.2.55.3.604688237.761.1243134237.659', '2025-02-15 10:00:00', 'SELECTED', 'NORMAL', 6, 1, 1, 5),
('ACC20250214007', '1.2.840.113619.2.55.3.604688237.761.1243134237.660', '2025-02-15 11:30:00', 'SELECTED', 'URGENT', 7, 2, 3, 6);

-- Rapports exemples avec les nouveaux champs
INSERT INTO reports (exam_id, findings, conclusion, validated, radiologue_id, created_at, updated_at) VALUES
(1, 'Examen normal sans anomalie détectée. Parenchyme pulmonaire d''aspect normal. Pas d''épanchement pleural.', 'Examen thoracique normal.', false, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(2, 'Présence d''une discopathie L4-L5 avec protrusion discale modérée. Pas de signe de conflit radiculaire.', 'Discopathie lombaire L4-L5.', false, 6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

COMMIT;

-- Étape 6: Configuration des séquences
\echo "Configuration des séquences..."

-- Réinitialiser les séquences
SELECT setval('modality_types_id_seq', 10, true);
SELECT setval('modalities_id_seq', 9, true);
SELECT setval('users_id_seq', 6, true);
SELECT setval('patients_id_seq', 7, true);
SELECT setval('procedure_catalogs_id_seq', 16, true);
SELECT setval('exams_id_seq', 7, true);
SELECT setval('reports_id_seq', 2, true);

\echo "=========================================="
\echo "Base de données radiology_db initialisée!"
\echo "=========================================="
\echo ""
\echo "Utilisateurs créés:"
\echo "- admin / admin123 (ADMIN)"
\echo "- dr_dupont / admin123 (MEDECIN)"
\echo "- dr_martin / admin123 (MEDECIN)"
\echo "- tech1 / admin123 (TECHNICIEN)"
\echo "- sec1 / admin123 (SECRETAIRE)"
\echo "- radio1 / admin123 (RADIOLOGUE)"
\echo ""
\echo "Patients: 7 patients créés (dont 2 mineurs)"
\echo "Examens: 7 examens créés (5 PLANNED, 2 SELECTED)"
\echo "Procédures: 16 procédures cataloguées"
\echo "Équipements: 9 modalités configurées"
\echo "=========================================="
