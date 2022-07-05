DROP TABLE workflows;
--;;

UPDATE event_types
SET category = 'workflow'
WHERE category = 'command' AND name = 'failed';
--;;

INSERT INTO event_types (category, name)
VALUES
('workflow', 'created'),
('workflow', 'updated');
--;;

DROP VIEW user_events;
--;;

DROP VIEW all_events;
--;;

ALTER TABLE events DROP COLUMN data;
--;;

ALTER TABLE events ADD COLUMN data TEXT;
--;;

CREATE VIEW all_events AS
SELECT e.id,
    e.model_id,
    e.data,
    e.emitted_at,
    e.emitted_by,
    e.ctx,
    (et.category::text || '/'::text) || et.name::text AS event_type
FROM events e
JOIN event_types et
    ON et.id = e.event_type_id;
--;;

CREATE VIEW user_events AS
SELECT ae.id,
    ae.event_type,
    ae.model_id,
    ae.data,
    ae.emitted_at,
    ae.emitted_by,
    ae.ctx,
    COALESCE(ut.user_id, ae.emitted_by) AS user_id
FROM all_events ae
LEFT JOIN artifacts a
    ON a.id = ae.model_id
LEFT JOIN file_versions fv
    ON fv.artifact_id = a.id
    OR fv.id = ae.model_id
LEFT JOIN files f
    ON f.id = COALESCE(fv.file_id, ae.model_id)
LEFT JOIN projects p
    ON p.id = COALESCE(f.project_id, ae.model_id)
LEFT JOIN teams t
    ON t.id = COALESCE(p.team_id, ae.model_id)
LEFT JOIN user_teams ut
    ON ut.team_id = t.id;
--;;
