-- Script to add worklist column to exams table safely
-- This script handles existing records by generating worklist values for them

-- Step 1: Add the worklist column as nullable first
ALTER TABLE exams ADD COLUMN worklist VARCHAR(255);

-- Step 2: Update existing records to generate worklist values
-- This will generate worklist IDs for existing exams in format WL-001, WL-002, etc.
UPDATE exams 
SET worklist = 'WL-' || LPAD(
    (SELECT COUNT(*) + 1 
     FROM exams e2 
     WHERE e2.id <= exams.id)::text, 
    3, '0'
)
WHERE worklist IS NULL;

-- Step 3: Make the column NOT NULL (only after all existing records have worklist values)
ALTER TABLE exams ALTER COLUMN worklist SET NOT NULL;

-- Step 4: Add unique constraint
ALTER TABLE exams ADD CONSTRAINT uk_exams_worklist UNIQUE (worklist);
