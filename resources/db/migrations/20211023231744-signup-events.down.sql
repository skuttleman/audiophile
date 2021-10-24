DELETE FROM events
WHERE event_type_id = (SELECT id
                       FROM event_types
                       WHERE category = 'user'
                         AND name = 'created');
--;;

DELETE FROM event_types
WHERE category = 'user'
  AND name = 'created';
--;;
