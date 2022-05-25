ALTER TABLE events
    ALTER COLUMN emitted_by SET NOT NULL,
    ADD CONSTRAINT events_emitted_by_fkey FOREIGN KEY (emitted_by) REFERENCES users(id);
--;;
