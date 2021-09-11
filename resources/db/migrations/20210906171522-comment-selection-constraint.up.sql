UPDATE comments
SET selection = NULL
WHERE comment_id IS NOT NULL;
--;;

ALTER TABLE comments
    ADD CONSTRAINT comments_nested_selection CHECK (selection IS NULL OR comment_id IS NULL);
--;;
