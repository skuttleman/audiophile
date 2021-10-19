(ns com.ben-allred.audiophile.backend.api.repositories.files.protocols)

(defprotocol IArtifactsExecutor
  "Abstraction for querying artifacts"
  (insert-artifact-access? [this artifact opts]
    "Pre-flight check for access to create the artifact.")
  (insert-artifact! [this artifact opts]
    "Inserts a new artifact. Returns primary id for new artifact")
  (find-by-artifact-id [this artifact-id opts]
    "Finds artifact by id.")
  (find-event-artifact [this artifact-id]
    "Finds event-ready artifact by id."))

(defprotocol IArtifactStore
  "An [[com.ben-allred.audiophile.backend.api.repositories.protocols/IKVStore]]
   implementation for storing and retrieving artifacts")

(defprotocol IFilesExecutor
  "Abstraction for querying files"
  (insert-file-access? [this file opts]
    "Pre-flight check for access to create the file.")
  (insert-file! [this file opts]
    "Inserts a file and version. Returns primary id for new file")
  (find-by-file-id [this file-id opts]
    "Lookup file by file-id. If `(:includes/versions? opts)` is true, returns all versions.
     Otherwise, it includes only most recent version.")
  (select-for-project [this project-id opts]
    "Finds all files for a project.")
  (find-event-file [this file-id]
    "Finds event-ready file by id."))

(defprotocol IFileVersionsExecutor
  "Abstraction for querying file-versions"
  (insert-version-access? [this version opts]
    "Pre-flight check for access to create the version.")
  (insert-version! [this version opts]
    "Inserts a new version for an existing file. Returns primary id for new version")
  (find-event-version [this version-id]
    "Finds event-ready version by id."))
