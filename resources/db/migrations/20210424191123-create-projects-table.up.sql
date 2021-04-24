CREATE TYPE TEAM_TYPE AS ENUM ('PERSONAL', 'COLLABORATIVE');
--;;

CREATE TABLE teams (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    "type" TEAM_TYPE NOT NULL,
    created_by UUID REFERENCES users NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);
--;;

CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    team_id UUID REFERENCES teams NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_by UUID REFERENCES users NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);
--;;

CREATE TABLE user_teams (
    user_id UUID REFERENCES users NOT NULL,
    team_id UUID REFERENCES teams NOT NULL,

    PRIMARY KEY(team_id, user_id)
);
--;;
