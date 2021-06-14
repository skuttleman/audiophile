DROP INDEX events_emitted_at;
--;;

DROP INDEX events_emitted_by;
--;;

DROP VIEW user_events;
--;;

DROP VIEW all_events;
--;;

CREATE VIEW all_events AS
    SELECT e.id, e.data, e.emitted_by, e.emitted_at, et.category || '/' || et.name AS event_type
    FROM events e
    INNER JOIN event_types et ON et.id = e.event_type_id;
--;;
