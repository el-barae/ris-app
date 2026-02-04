# Migration pour autoriser le statut SELECTED

## Instructions

Exécutez ce script SQL dans votre base de données PostgreSQL:

```sql
-- Script pour mettre à jour la contrainte exams_status_check
-- afin d'autoriser le statut SELECTED

-- Supprimer l'ancienne contrainte
ALTER TABLE exams DROP CONSTRAINT IF EXISTS exams_status_check;

-- Créer la nouvelle contrainte avec tous les statuts possibles
ALTER TABLE exams ADD CONSTRAINT exams_status_check 
CHECK (status IN ('PLANNED', 'SELECTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'));
```

## Comment exécuter:

1. Ouvrez pgAdmin ou votre client PostgreSQL préféré
2. Connectez-vous à la base de données `radiology_db`
3. Copiez et exécutez le script SQL ci-dessus

Une fois la migration effectuée, l'application pourra utiliser le statut `SELECTED` sans erreur de contrainte.
