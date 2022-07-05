CREATE TABLE workflows (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    status VARCHAR(50) NOT NULL DEFAULT 'init',
    data TEXT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);
--;;

DELETE FROM events
WHERE event_type_id IN (SELECT id
                        FROM event_types
                        WHERE category = 'workflow' AND name IN ('created', 'updated'));
--;;

DELETE FROM event_types
WHERE category = 'workflow' AND name IN ('created', 'updated');
--;;

UPDATE event_types
SET category = 'command'
WHERE category = 'workflow' AND name = 'failed';
--;;

DROP VIEW user_events;
--;;

DROP VIEW all_events;
--;;

ALTER TABLE events DROP COLUMN data;
--;;

ALTER TABLE events ADD COLUMN data JSONB;
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
