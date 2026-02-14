-- ========================================
-- Correction du problème Hibernate avec la colonne hospital_id
-- ========================================

BEGIN;

-- 1. Mettre à jour les ordres qui ont hospital_id NULL avec une valeur par défaut
-- (Utilise l'hôpital avec ID=1 comme défaut)
UPDATE orders 
SET hospital_id = 1 
WHERE hospital_id IS NULL;

-- 2. Créer un trigger pour garantir que hospital_id n'est jamais NULL
CREATE OR REPLACE FUNCTION ensure_hospital_id()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.hospital_id IS NULL THEN
        NEW.hospital_id = 1; -- Hôpital par défaut
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 3. Appliquer le trigger
CREATE TRIGGER trg_orders_ensure_hospital_id 
    BEFORE INSERT OR UPDATE ON orders 
    FOR EACH ROW 
    EXECUTE FUNCTION ensure_hospital_id();

-- 4. Afficher le résultat
SELECT 
    'Mise à jour terminée' as status,
    COUNT(*) as total_orders,
    COUNT(hospital_id) as orders_with_hospital,
    COUNT(*) - COUNT(hospital_id) as orders_fixed
FROM orders;

COMMIT;
