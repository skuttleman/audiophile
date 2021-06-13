ALTER TABLE artifacts
    DROP COLUMN created_by;
--;;

ALTER TABLE file_versions
    DROP COLUMN created_by;
--;;

ALTER TABLE files
    DROP COLUMN created_by;
--;;

ALTER TABLE projects
    DROP COLUMN created_by;
--;;

ALTER TABLE teams
    DROP COLUMN created_by;
--;;
