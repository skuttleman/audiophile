CREATE TABLE event_types (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL
);
--;;

CREATE UNIQUE INDEX event_types_category_name_unique on event_types (category, name);
--;;

INSERT INTO event_types (category, name) VALUES
    ('command', 'failed'),
    ('artifact', 'created'),
    ('artifact', 'archived'),
    ('file', 'created'),
    ('file-version', 'created'),
    ('user', 'authenticated'),
    ('team', 'user-joined');
--;;

CREATE TABLE events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_type_id UUID REFERENCES event_types NOT NULL,
    model_id UUID NOT NULL,
    data JSONB,
    emitted_by UUID REFERENCES users NOT NULL,
    emitted_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);
--;;

CREATE VIEW all_events AS
    SELECT e.id, e.data, e.emitted_by, e.emitted_at, et.category || '/' || et.name AS event_type
    FROM events e
    INNER JOIN event_types et ON et.id = e.event_type_id;
--;;
