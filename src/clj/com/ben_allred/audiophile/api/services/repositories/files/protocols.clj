(ns com.ben-allred.audiophile.api.services.repositories.files.protocols)

(defprotocol IFilesExecutor
  "Abstraction for querying files"
  (insert-file! [this file opts]
    "")
  (insert-version! [this version opts]
    "")
  (insert-artifact! [this artifact opts]
    "")
  ;; with latest version, or with all versions
  (find-by-file-id [this file-id opts]
    "")
  (find-by-artifact-id [this artifact-id opts]
    "")
  (select-for-project [this project-id opts]
    ""))
