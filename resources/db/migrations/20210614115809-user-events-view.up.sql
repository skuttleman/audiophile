DROP VIEW all_events;
--;;

CREATE VIEW all_events AS
    SELECT e.id, e.model_id, e.data, e.emitted_by, e.emitted_at, et.category || '/' || et.name AS event_type
    FROM events e
    INNER JOIN event_types et ON et.id = e.event_type_id;
--;;

CREATE VIEW user_events AS
    SELECT ae.event_type, ae.model_id, ae.data, ae.emitted_at,
           ae.emitted_by, COALESCE(ut.user_id, ae.emitted_by) AS user_id
    FROM all_events ae
    LEFT JOIN artifacts a ON a.id = ae.model_id
    LEFT JOIN file_versions fv ON fv.artifact_id = a.id OR fv.id = ae.model_id
    LEFT JOIN files f ON f.id = COALESCE(fv.file_id, ae.model_id)
    LEFT JOIN projects p ON p.id = COALESCE(f.project_id, ae.model_id)
    LEFT JOIN teams t ON t.id = COALESCE(p.team_id, ae.model_id)
    LEFT JOIN user_teams ut ON ut.team_id = t.id;
--;;

CREATE INDEX events_emitted_by ON events (emitted_by);
--;;

CREATE INDEX events_emitted_at ON events (emitted_at);
--;;
