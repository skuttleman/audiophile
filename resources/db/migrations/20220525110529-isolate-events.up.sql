ALTER TABLE events
    DROP CONSTRAINT events_emitted_by_fkey,
    ALTER COLUMN emitted_by DROP NOT NULL;
--;;
