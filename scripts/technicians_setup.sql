-- Script pour créer des techniciens et les associer aux modalités CT, MRI, XR, US

-- Création des utilisateurs pour les techniciens
INSERT INTO users (username, password, email, first_name, last_name, role, active, hospital_id, created_at, updated_at) VALUES
('tech_jean_martin', '$2a$10$tl/2bh7DItVZNwMEGypuqeoLxCB3oH79SJzb.chw885ZqB9hcEss2', 'jean.martin@radiology.com', 'Jean', 'Martin', 'TECHNICIEN', true, 1, NOW(), NOW()),
('tech_marie_dubois', '$2a$10$tl/2bh7DItVZNwMEGypuqeoLxCB3oH79SJzb.chw885ZqB9hcEss2', 'marie.dubois@radiology.com', 'Marie', 'Dubois', 'TECHNICIEN', true, 1, NOW(), NOW()),
('tech_pierre_bernard', '$2a$10$tl/2bh7DItVZNwMEGypuqeoLxCB3oH79SJzb.chw885ZqB9hcEss2', 'pierre.bernard@radiology.com', 'Pierre', 'Bernard', 'TECHNICIEN', true, 1, NOW(), NOW()),
('tech_sophie_petit', '$2a$10$tl/2bh7DItVZNwMEGypuqeoLxCB3oH79SJzb.chw885ZqB9hcEss2', 'sophie.petit@radiology.com', 'Sophie', 'Petit', 'TECHNICIEN', true, 1, NOW(), NOW()),
('tech_claude_robert', '$2a$10$tl/2bh7DItVZNwMEGypuqeoLxCB3oH79SJzb.chw885ZqB9hcEss2', 'claude.robert@radiology.com', 'Claude', 'Robert', 'TECHNICIEN', true, 1, NOW(), NOW());

-- Insertion des techniciens
INSERT INTO technicians (employee_id, first_name, last_name, phone, email, specialization, is_active, created_at, updated_at, user_id) VALUES
('TECH001', 'Jean', 'Martin', '0612345678', 'jean.martin@radiology.com', 'CT et Radiographie', true, NOW(), NOW(), 7),
('TECH002', 'Marie', 'Dubois', '0623456789', 'marie.dubois@radiology.com', 'IRM et Echographie', true, NOW(), NOW(), 8),
('TECH003', 'Pierre', 'Bernard', '0634567890', 'pierre.bernard@radiology.com', 'Radiographie generale', true, NOW(), NOW(), 9),
('TECH004', 'Sophie', 'Petit', '0645678901', 'sophie.petit@radiology.com', 'CT et IRM', true, NOW(), NOW(), 10),
('TECH005', 'Claude', 'Robert', '0656789012', 'claude.robert@radiology.com', 'Echographie', true, NOW(), NOW(), 11);

-- Association des techniciens aux types de modalités
-- Jean Martin (TECH001) - CT et XR (CR/DX)
INSERT INTO technician_modality_types (technician_id, modality_type_id) VALUES
(1, 1),  -- CT
(1, 9),  -- CR
(1, 10); -- DX

-- Marie Dubois (TECH002) - MRI et US
INSERT INTO technician_modality_types (technician_id, modality_type_id) VALUES
(2, 2),  -- MRI
(2, 4);  -- US

-- Pierre Bernard (TECH003) - XR (CR/DX)
INSERT INTO technician_modality_types (technician_id, modality_type_id) VALUES
(3, 9),  -- CR
(3, 10); -- DX

-- Sophie Petit (TECH004) - CT et MRI
INSERT INTO technician_modality_types (technician_id, modality_type_id) VALUES
(4, 1),  -- CT
(4, 2);  -- MRI

-- Claude Robert (TECH005) - US
INSERT INTO technician_modality_types (technician_id, modality_type_id) VALUES
(5, 4);  -- US

-- Vérification
SELECT 
    t.employee_id,
    t.first_name,
    t.last_name,
    t.specialization,
    STRING_AGG(mt.code, ', ') as modalites
FROM technicians t
LEFT JOIN technician_modality_types tmt ON t.id = tmt.technician_id
LEFT JOIN modality_types mt ON tmt.modality_type_id = mt.id
WHERE t.is_active = true
GROUP BY t.id, t.employee_id, t.first_name, t.last_name, t.specialization
ORDER BY t.last_name, t.first_name;
