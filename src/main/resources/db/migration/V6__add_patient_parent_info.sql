-- Add parent information columns to patients table
ALTER TABLE patients ADD COLUMN parent_first_name VARCHAR(100);
ALTER TABLE patients ADD COLUMN parent_last_name VARCHAR(100);
ALTER TABLE patients ADD COLUMN parent_phone VARCHAR(20);
ALTER TABLE patients ADD COLUMN parent_relationship VARCHAR(50);

-- Add indexes for performance
CREATE INDEX idx_patient_parent_last_name ON patients(parent_last_name);
CREATE INDEX idx_patient_parent_phone ON patients(parent_phone);
