-- ========================================
-- Création de la table procedure_steps
-- ========================================
BEGIN;

-- Créer la table procedure_steps
CREATE TABLE IF NOT EXISTS procedure_steps (
    id BIGSERIAL PRIMARY KEY,
    procedure_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    step_order INTEGER,
    estimated_duration_minutes INTEGER,
    actual_duration_minutes INTEGER,
    is_required BOOLEAN DEFAULT true,
    is_completed BOOLEAN DEFAULT false,
    instructions TEXT,
    completion_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (procedure_id) REFERENCES procedures(id) ON DELETE CASCADE
);

-- Créer les index
CREATE INDEX IF NOT EXISTS idx_procedure_steps_procedure_id ON procedure_steps(procedure_id);
CREATE INDEX IF NOT EXISTS idx_procedure_steps_step_order ON procedure_steps(step_order);
CREATE INDEX IF NOT EXISTS idx_procedure_steps_is_completed ON procedure_steps(is_completed);
CREATE INDEX IF NOT EXISTS idx_procedure_steps_is_required ON procedure_steps(is_required);

-- Créer le trigger pour updated_at
CREATE TRIGGER update_procedure_steps_updated_at 
    BEFORE UPDATE ON procedure_steps 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

COMMIT;
