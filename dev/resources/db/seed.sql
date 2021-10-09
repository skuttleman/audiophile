INSERT INTO users
(id, first_name, last_name, handle, email, mobile_number)
VALUES
('2ba063ad-f30a-4f07-8618-c7d32c8d699b', 'Ben', 'Allred', 'skuttleman', 'skuttleman@gmail.com', '4437979211');
--;;

INSERT INTO teams
(name, type)
VALUES
('My Personal Projects', 'PERSONAL');
--;;

INSERT INTO user_teams
(user_id, team_id)
VALUES
((SELECT id FROM users), (SELECT id FROM teams));
--;;
