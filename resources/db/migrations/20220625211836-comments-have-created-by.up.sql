ALTER TABLE comments ADD COLUMN created_by UUID REFERENCES users;
--;;

UPDATE comments c
SET created_by = (SELECT u.id
                  FROM users u
                  INNER JOIN all_events ae ON ae.model_id = c.id AND ae.emitted_by = u.id
                  WHERE ae.event_type = 'comment/created');
--;;

DELETE FROM comments
WHERE created_by IS NULL;
--;;

ALTER TABLE comments ALTER COLUMN created_by SET NOT NULL;
--;;
