-- Migration script to add Hospital entity and update existing tables
-- Run this script after updating the application code

-- Create hospitals table
CREATE TABLE IF NOT EXISTS hospitals (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address TEXT,
    city VARCHAR(255),
    postal_code VARCHAR(50),
    country VARCHAR(255),
    phone VARCHAR(50),
    email VARCHAR(255),
    website VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add hospital_id columns to existing tables (nullable initially)
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS hospital_id BIGINT;
ALTER TABLE modalities ADD COLUMN IF NOT EXISTS hospital_id BIGINT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS hospital_id BIGINT;

-- Add foreign key constraints
ALTER TABLE rooms ADD CONSTRAINT fk_rooms_hospital 
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id);
    
ALTER TABLE modalities ADD CONSTRAINT fk_modalities_hospital 
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id);
    
ALTER TABLE users ADD CONSTRAINT fk_users_hospital 
    FOREIGN KEY (hospital_id) REFERENCES hospitals(id);

-- Insert a default hospital for existing data
INSERT INTO hospitals (name, city, country, created_at, updated_at)
VALUES ('Hôpital par défaut', 'Ville par défaut', 'Maroc', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- Update existing records to reference the default hospital
UPDATE rooms SET hospital_id = (SELECT id FROM hospitals WHERE name = 'Hôpital par défaut') 
WHERE hospital_id IS NULL;

UPDATE modalities SET hospital_id = (SELECT id FROM hospitals WHERE name = 'Hôpital par défaut') 
WHERE hospital_id IS NULL;

UPDATE users SET hospital_id = (SELECT id FROM hospitals WHERE name = 'Hôpital par défaut') 
WHERE hospital_id IS NULL;

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_rooms_hospital_id ON rooms(hospital_id);
CREATE INDEX IF NOT EXISTS idx_modalities_hospital_id ON modalities(hospital_id);
CREATE INDEX IF NOT EXISTS idx_users_hospital_id ON users(hospital_id);
CREATE INDEX IF NOT EXISTS idx_hospitals_name ON hospitals(name);
CREATE INDEX IF NOT EXISTS idx_hospitals_city ON hospitals(city);
CREATE INDEX IF NOT EXISTS idx_hospitals_country ON hospitals(country);
