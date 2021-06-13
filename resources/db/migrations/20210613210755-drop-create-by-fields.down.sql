ALTER TABLE teams
    ADD COLUMN created_by UUID REFERENCES users;
--;;

UPDATE teams t
SET created_by = (SELECT e.emitted_by
                  FROM events e
                  INNER JOIN event_types et ON et.id = e.event_type_id
                  WHERE e.model_id = t.id
                    AND et.category = 'team'
                    AND et.name = 'created');
--;;

ALTER TABLE teams
    ALTER COLUMN created_by SET NOT NULL;
--;;

ALTER TABLE projects
    ADD COLUMN created_by UUID REFERENCES users;
--;;

UPDATE projects p
SET created_by = (SELECT e.emitted_by
                  FROM events e
                  INNER JOIN event_types et ON et.id = e.event_type_id
                  WHERE e.model_id = p.id
                    AND et.category = 'project'
                    AND et.name = 'created');
--;;

ALTER TABLE projects
    ALTER COLUMN created_by SET NOT NULL;
--;;

ALTER TABLE files
    ADD COLUMN created_by UUID REFERENCES users;
--;;

UPDATE files f
SET created_by = (SELECT e.emitted_by
                  FROM events e
                  INNER JOIN event_types et ON et.id = e.event_type_id
                  WHERE e.model_id = f.id
                    AND et.category = 'file'
                    AND et.name = 'created');
--;;

ALTER TABLE files
    ALTER COLUMN created_by SET NOT NULL;
--;;

ALTER TABLE file_versions
    ADD COLUMN created_by UUID REFERENCES users;
--;;

UPDATE file_versions fv
SET created_by = (SELECT e.emitted_by
                  FROM events e
                  INNER JOIN event_types et ON et.id = e.event_type_id
                  WHERE e.model_id = fv.id
                    AND et.category = 'file-version'
                    AND et.name = 'created');
--;;

ALTER TABLE file_versions
    ALTER COLUMN created_by SET NOT NULL;
--;;

ALTER TABLE artifacts
    ADD COLUMN created_by UUID REFERENCES users;
--;;

UPDATE artifacts a
SET created_by = (SELECT e.emitted_by
                  FROM events e
                  INNER JOIN event_types et ON et.id = e.event_type_id
                  WHERE e.model_id = a.id
                    AND et.category = 'artifact'
                    AND et.name = 'created');
--;;

ALTER TABLE artifacts
    ALTER COLUMN created_by SET NOT NULL;
--;;
