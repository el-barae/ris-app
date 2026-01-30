-- Remove examType column and add modality_type_id foreign key
-- First drop the examType column
ALTER TABLE exams DROP COLUMN IF EXISTS exam_type;

-- Replace modality column with modality_type_id foreign key
ALTER TABLE exams DROP COLUMN IF EXISTS modality;
ALTER TABLE exams ADD COLUMN modality_type_id BIGINT NOT NULL;

-- Add modality_id foreign key for Modality entity (nullable)
ALTER TABLE exams ADD COLUMN modality_id BIGINT;

-- Add foreign key constraints
ALTER TABLE exams ADD CONSTRAINT fk_exam_modality_type 
    FOREIGN KEY (modality_type_id) REFERENCES modality_types(id);

ALTER TABLE exams ADD CONSTRAINT fk_exam_modality 
    FOREIGN KEY (modality_id) REFERENCES modalities(id);

-- Create indexes for performance
CREATE INDEX idx_exam_modality_type_id ON exams(modality_type_id);
CREATE INDEX idx_exam_modality_id ON exams(modality_id);
