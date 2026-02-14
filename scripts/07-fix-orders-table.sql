-- ========================================
-- Nettoyage et création de la table orders
-- ========================================

BEGIN;

-- Supprimer les données corrompues de la table orders si elle existe
DELETE FROM orders WHERE accession_number LIKE 'ORD-1771085214298-509CD691' OR study_instance_uid LIKE '1.2.276.0.7230010.3.1.2.493970273.1.1771085214.838227%';

-- Créer la table orders si elle n'existe pas
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    study_instance_uid VARCHAR(255) NOT NULL UNIQUE,
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

-- Créer les index pour optimiser les performances
CREATE INDEX IF NOT EXISTS idx_orders_study_instance_uid ON orders(study_instance_uid);
CREATE INDEX IF NOT EXISTS idx_orders_accession_number ON orders(accession_number);
CREATE INDEX IF NOT EXISTS idx_orders_hospital_id ON orders(hospital_id);
CREATE INDEX IF NOT EXISTS idx_orders_doctor_id ON orders(doctor_id);
CREATE INDEX IF NOT EXISTS idx_orders_patient_id ON orders(patient_id);
CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);

-- Créer le trigger pour mettre à jour le timestamp
CREATE OR REPLACE FUNCTION update_orders_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_orders_updated_at BEFORE UPDATE ON orders 
    FOR EACH ROW EXECUTE FUNCTION update_orders_updated_at();

COMMIT;
