-- Recovery script for worklist column (after first ALTER TABLE already ran)

-- Step 1: Update existing records to generate worklist values
UPDATE exams 
SET worklist = 'WL-' || LPAD(
    (SELECT COUNT(*) + 1 
     FROM exams e2 
     WHERE e2.id <= exams.id)::text, 
    3, '0'
)
WHERE worklist IS NULL;

-- Step 2: Make the column NOT NULL
ALTER TABLE exams ALTER COLUMN worklist SET NOT NULL;

-- Step 3: Add unique constraint
ALTER TABLE exams ADD CONSTRAINT uk_exams_worklist UNIQUE (worklist);
