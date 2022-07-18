CREATE INDEX team_invitations_email on team_invitations (email);
--;;

DELETE FROM team_invitations;
--;;

ALTER TABLE team_invitations
    ADD COLUMN invited_by UUID REFERENCES users NOT NULL;
--;;
