DELETE FROM events
WHERE event_type_id = (SELECT id
                       FROM event_types
                       WHERE category = 'comment');
--;;

DELETE FROM event_types
WHERE category = 'comment';
--;;

DROP TABLE comments;
--;;
