-- Script pour créer les utilisateurs manquants pour les techniciens

-- Création des utilisateurs pour les techniciens sans compte
INSERT INTO users (username, password, email, first_name, last_name, role, active, hospital_id, created_at, updated_at) VALUES
('tech_jean_martin', '$2a$10$tl/2bh7DItVZNwMEGypuqeoLxCB3oH79SJzb.chw885ZqB9hcEss2', 'jean.martin@radiology.com', 'Jean', 'Martin', 'TECHNICIEN', true, 1, NOW(), NOW()),
('tech_marie_dubois', '$2a$10$tl/2bh7DItVZNwMEGypuqeoLxCB3oH79SJzb.chw885ZqB9hcEss2', 'marie.dubois@radiology.com', 'Marie', 'Dubois', 'TECHNICIEN', true, 1, NOW(), NOW()),
('tech_pierre_bernard', '$2a$10$tl/2bh7DItVZNwMEGypuqeoLxCB3oH79SJzb.chw885ZqB9hcEss2', 'pierre.bernard@radiology.com', 'Pierre', 'Bernard', 'TECHNICIEN', true, 1, NOW(), NOW()),
('tech_sophie_petit', '$2a$10$tl/2bh7DItVZNwMEGypuqeoLxCB3oH79SJzb.chw885ZqB9hcEss2', 'sophie.petit@radiology.com', 'Sophie', 'Petit', 'TECHNICIEN', true, 1, NOW(), NOW()),
('tech_claude_robert', '$2a$10$tl/2bh7DItVZNwMEGypuqeoLxCB3oH79SJzb.chw885ZqB9hcEss2', 'claude.robert@radiology.com', 'Claude', 'Robert', 'TECHNICIEN', true, 1, NOW(), NOW());

-- Mise à jour des techniciens avec leurs user_id correspondants
UPDATE technicians SET user_id = 7 WHERE employee_id = 'TECH001'; -- Jean Martin
UPDATE technicians SET user_id = 8 WHERE employee_id = 'TECH002'; -- Marie Dubois  
UPDATE technicians SET user_id = 9 WHERE employee_id = 'TECH003'; -- Pierre Bernard
UPDATE technicians SET user_id = 10 WHERE employee_id = 'TECH004'; -- Sophie Petit
UPDATE technicians SET user_id = 11 WHERE employee_id = 'TECH005'; -- Claude Robert

-- Vérification
SELECT 
    t.id as technician_id,
    t.employee_id,
    t.first_name,
    t.last_name,
    u.username,
    u.email,
    u.role
FROM technicians t
JOIN users u ON t.user_id = u.id
ORDER BY t.last_name, t.first_name;
