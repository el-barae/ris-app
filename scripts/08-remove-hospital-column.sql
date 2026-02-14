-- ========================================
-- Suppression de la colonne hospital_id de la table orders
-- ========================================

-- ATTENTION : Ce script va supprimer la colonne hospital_id et modifier l'application
-- pour ne plus utiliser l'hôpital (sera automatiquement déterminé par l'utilisateur)

BEGIN;

-- Créer une table de sauvegarde des données orders
CREATE TABLE orders_backup AS SELECT * FROM orders;

-- Supprimer les contraintes étrangères
ALTER TABLE orders DROP CONSTRAINT IF EXISTS fk_orders_hospital;

-- Supprimer la colonne hospital_id
ALTER TABLE orders DROP COLUMN IF EXISTS hospital_id;

-- Supprimer les index liés à hospital_id
DROP INDEX IF EXISTS idx_orders_hospital_id;

-- Mettre à jour les autres index si nécessaire
-- (Les index restants seront automatiquement recréés si besoin)

COMMIT;

-- ========================================
-- Instructions pour la mise à jour du code Java
-- ========================================

/*
Étapes à suivre dans le code Java :

1. Dans l'entité Order.java :
   - Supprimer l'attribut "hospital"
   - Supprimer l'annotation @JoinColumn pour hospital_id
   - Supprimer l'import de l'entité Hospital

2. Dans OrderView.java :
   - Supprimer la logique de sélection d'hôpital
   - Modifier getCurrentUserHospital() pour retourner null ou supprimer la méthode
   - Simplifier la validation en retirant la vérification de l'hôpital

3. Dans les services/repositories :
   - Supprimer les références à l'hôpital dans les requêtes

4. Tester l'application sans la gestion hospitalière
*/
