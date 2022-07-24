ALTER TABLE file_versions
ADD COLUMN selected_at TIMESTAMP WITHOUT TIME ZONE DEFAULT now();
--;;

UPDATE file_versions
SET selected_at = created_at;
--;;

ALTER TABLE file_versions
ALTER COLUMN selected_at SET NOT NULL;
--;;
