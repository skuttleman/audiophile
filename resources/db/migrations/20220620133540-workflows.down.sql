DELETE FROM events
WHERE event_type_id = (SELECT id
                       FROM event_types
                       WHERE category = 'workflow' AND
                             name = 'completed');
--;;

DELETE FROM event_types
WHERE category = 'workflow' AND
      name = 'completed';
--;;

DROP TABLE workflows;
--;;
