(ns com.ben-allred.audiophile.backend.api.repositories.files.protocols)

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
    "Finds all files for a project.")
  (find-event-file [this file-id]
    "Finds event-ready file by id.")
  (find-event-version [this version-id]
    "Finds event-ready version by id.")
  (find-event-artifact [this artifact-id]
    "Finds event-ready artifact by id."))

(defprotocol IFilesEventEmitter
  "Abstraction for emitting events related to files"
  (file-created! [this user-id file ctx]
    "Emitted when an file is created in the system")
  (version-created! [this user-id version ctx]
    "Emitted when an version is created in the system")
  (artifact-created! [this user-id artifact ctx]
    "Emitted when an artifact is created in the system"))
