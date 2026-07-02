-- Custom migration: Ensure each PATIENT can belong to at most one group.
-- This is a partial unique index — it only applies to PATIENT users.
-- NURSE and ADMIN users are not restricted.

-- Create the partial unique index.
-- PostgreSQL doesn't allow direct WHERE on relation, so we create a function-based approach.
-- Instead, create a view or trigger. Simpler: use a UNIQUE partial index on group_members.

-- Step 1: Add a trigger function that checks before INSERT/UPDATE
CREATE OR REPLACE FUNCTION check_patient_single_group()
RETURNS TRIGGER AS $$
DECLARE
    user_role TEXT;
    existing_count INT;
BEGIN
    SELECT role INTO user_role FROM users WHERE id = NEW."userId";

    IF user_role = 'PATIENT' THEN
        SELECT COUNT(*) INTO existing_count
        FROM group_members
        WHERE "userId" = NEW."userId";

        IF (TG_OP = 'INSERT' AND existing_count > 0) OR
           (TG_OP = 'UPDATE' AND existing_count > 1) THEN
            RAISE EXCEPTION 'PATIENT can only belong to one group';
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Step 2: Create the trigger
DROP TRIGGER IF EXISTS enforce_patient_single_group ON group_members;
CREATE TRIGGER enforce_patient_single_group
    BEFORE INSERT OR UPDATE ON group_members
    FOR EACH ROW
    EXECUTE FUNCTION check_patient_single_group();
