CREATE TABLE comments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    file_version_id UUID REFERENCES file_versions NOT NULL,
    body TEXT NOT NULL,
    selection NUMRANGE,
    comment_id UUID REFERENCES comments,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now()
);
--;;

INSERT INTO event_types (category, name) VALUES
    ('comment', 'created');
--;;
