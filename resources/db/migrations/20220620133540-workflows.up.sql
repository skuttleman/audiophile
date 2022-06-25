CREATE TABLE workflows (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    status VARCHAR(50) NOT NULL DEFAULT 'init',
    data TEXT NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);
--;;

INSERT INTO event_types (category, name)
VALUES ('workflow', 'completed');
--;;
