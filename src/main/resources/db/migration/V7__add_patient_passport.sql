-- Add passport identity column to patients table
ALTER TABLE patients ADD COLUMN passport_number VARCHAR(50);

-- Add index for performance
CREATE INDEX idx_patient_passport_number ON patients(passport_number);
