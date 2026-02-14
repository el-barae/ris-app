-- Création de la table procedures pour stocker les procédures spécifiques aux examens
BEGIN;

-- Table des procédures (instances spécifiques créées à partir des catalogues)
CREATE TABLE procedures (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    procedure_code VARCHAR(20),
    description TEXT,
    region VARCHAR(50),
    laterality VARCHAR(20),
    contrast_required BOOLEAN DEFAULT false,
    contrast_type VARCHAR(50),
    injection_rate DECIMAL(10,2),
    injection_volume DECIMAL(10,2),
    special_instructions TEXT,
    is_active BOOLEAN DEFAULT true,
    scheduled_duration_minutes INTEGER DEFAULT 30,
    modality_type_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (modality_type_id) REFERENCES modality_types(id)
);

-- Mettre à jour la table exams pour pointer vers procedures au lieu de procedure_catalogs
ALTER TABLE exams DROP CONSTRAINT IF EXISTS exams_procedure_id_fkey;
ALTER TABLE exams ADD CONSTRAINT exams_procedure_id_fkey 
    FOREIGN KEY (procedure_id) REFERENCES procedures(id);

-- Index pour optimiser les performances
CREATE INDEX idx_procedures_code ON procedures(procedure_code);
CREATE INDEX idx_procedures_modality_type ON procedures(modality_type_id);
CREATE INDEX idx_procedures_region ON procedures(region);

-- Trigger pour mettre à jour les timestamps
CREATE TRIGGER update_procedures_updated_at BEFORE UPDATE ON procedures 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMIT;
