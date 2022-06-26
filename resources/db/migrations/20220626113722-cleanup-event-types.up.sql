WITH old_event_type_ids (id) AS (
    SELECT id
    FROM event_types
    WHERE category IN ('artifact', 'file-version', 'file', 'project', 'team', 'comment', 'user') AND
          name = 'created')
DELETE FROM events
WHERE event_type_id IN (SELECT id FROM old_event_type_ids);
--;;

DELETE FROM event_types
WHERE category IN ('artifact', 'file-version', 'file', 'project', 'team', 'comment', 'user') AND
      name = 'created';
--;;
