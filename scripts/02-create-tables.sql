-- ========================================
-- Création des tables pour l'application radiologique
-- ========================================
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

-- Table des hôpitaux
CREATE TABLE hospitals (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address TEXT,
    city VARCHAR(50),
    postal_code VARCHAR(20),
    country VARCHAR(50),
    phone VARCHAR(20),
    email VARCHAR(100),
    website VARCHAR(100),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des ordres
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    study_instance_uid VARCHAR(64) NOT NULL UNIQUE,
    accession_number VARCHAR(50) NOT NULL UNIQUE,
    hospital_id BIGINT NOT NULL,
    doctor_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id),
    FOREIGN KEY (doctor_id) REFERENCES users(id),
    FOREIGN KEY (patient_id) REFERENCES patients(id)
);

-- Table des procédures (spécifiques aux examens)
CREATE TABLE procedures (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    procedure_code VARCHAR(20),
    modality_type_id BIGINT,
    region VARCHAR(50),
    laterality VARCHAR(10),
    contrast_required BOOLEAN DEFAULT false,
    contrast_type VARCHAR(50),
    injection_rate DECIMAL(10,2),
    injection_volume DECIMAL(10,2),
    delay_seconds INTEGER,
    preparation_instructions TEXT,
    description TEXT,
    is_active BOOLEAN DEFAULT true,
    is_emergency BOOLEAN DEFAULT false,
    notes TEXT,
    special_instructions TEXT,
    scheduled_duration_minutes INTEGER,
    actual_duration_minutes INTEGER,
    procedure_catalog_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (modality_type_id) REFERENCES modality_types(id),
    FOREIGN KEY (procedure_catalog_id) REFERENCES procedure_catalogs(id)
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
    order_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(id),
    FOREIGN KEY (medecin_id) REFERENCES users(id),
    FOREIGN KEY (procedure_id) REFERENCES procedures(id),
    FOREIGN KEY (modality_type_id) REFERENCES modality_types(id),
    FOREIGN KEY (modality_id) REFERENCES modalities(id),
    FOREIGN KEY (order_id) REFERENCES orders(id)
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

-- Créer les index pour optimiser les performances
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

CREATE INDEX idx_hospitals_name ON hospitals(name);
CREATE INDEX idx_hospitals_city ON hospitals(city);
CREATE INDEX idx_hospitals_active ON hospitals(is_active);

CREATE INDEX idx_orders_accession_number ON orders(accession_number);
CREATE INDEX idx_orders_study_uid ON orders(study_instance_uid);
CREATE INDEX idx_orders_hospital_id ON orders(hospital_id);
CREATE INDEX idx_orders_doctor_id ON orders(doctor_id);
CREATE INDEX idx_orders_patient_id ON orders(patient_id);

CREATE INDEX idx_procedures_code ON procedures(procedure_code);
CREATE INDEX idx_procedures_modality_type ON procedures(modality_type_id);
CREATE INDEX idx_procedures_region ON procedures(region);
CREATE INDEX idx_procedures_catalog_id ON procedures(procedure_catalog_id);

CREATE INDEX idx_exams_order_id ON exams(order_id);

CREATE INDEX idx_modality_types_code ON modality_types(code);

-- Créer les triggers pour mettre à jour les timestamps
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

CREATE TRIGGER update_hospitals_updated_at BEFORE UPDATE ON hospitals 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_orders_updated_at BEFORE UPDATE ON orders 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_procedures_updated_at BEFORE UPDATE ON procedures 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_exams_updated_at BEFORE UPDATE ON exams 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_reports_updated_at BEFORE UPDATE ON reports 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_procedure_catalogs_updated_at BEFORE UPDATE ON procedure_catalogs 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_modalities_updated_at BEFORE UPDATE ON modalities 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_modality_types_updated_at BEFORE UPDATE ON modality_types 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();


COMMIT;