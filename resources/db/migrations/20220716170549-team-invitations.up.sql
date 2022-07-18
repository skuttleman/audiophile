CREATE TYPE TEAM_INVITATION_STATUS AS ENUM ('PENDING', 'ACCEPTED', 'REJECTED', 'REVOKED');
--;;

CREATE TABLE team_invitations (
    team_id UUID REFERENCES teams NOT NULL,
    email VARCHAR(255) NOT NULL,
    status TEAM_INVITATION_STATUS NOT NULL DEFAULT 'PENDING'::TEAM_INVITATION_STATUS,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),

    PRIMARY KEY(team_id, email)
);
--;;

ALTER TABLE team_invitations
    ADD CONSTRAINT team_invitations_valid_email CHECK (email ~ '^[a-z\-\+_0-9\.]+@[a-z\-\+_0-9]+\.[a-z\-\+_0-9\.]+$');
--;;
