-- Update existing patients with Gender.OTHER to Gender.MALE
-- This migration handles the removal of OTHER gender option
UPDATE patients SET gender = 'MALE' WHERE gender = 'OTHER';
