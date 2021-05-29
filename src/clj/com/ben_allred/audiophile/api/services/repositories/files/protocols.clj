(ns com.ben-allred.audiophile.api.services.repositories.files.protocols)

(defprotocol IFilesExecutor
  "Abstraction for querying files"
  (insert-file! [this file opts]
    "Inserts a file and version. Returns primary id for new file")
  (insert-version! [this version opts]
    "Inserts a new version for an existing file. Returns primary id for new version")
  (insert-artifact! [this artifact opts]
    "Inserts a new artifact. Returns primary id for new artifact")
  (find-by-file-id [this file-id opts]
    "Lookup file by file-id. If `(:includes/versions? opts)` is true, returns all versions.
     Otherwise includes only most recent version.")
  (find-by-artifact-id [this artifact-id opts]
    "Finds artifact by id.")
  (select-for-project [this project-id opts]
    "Finds all files for a project."))
