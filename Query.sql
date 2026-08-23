ALTER TABLE course ADD COLUMN title VARCHAR(10) NOT NULL DEFAULT '';

SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'course';