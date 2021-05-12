(ns com.ben-allred.audiophile.api.services.interactors.protocols)

(defprotocol IAccessor
  "Abstraction for interacting with a repository"
  (query-one [this opts]
    "Queries one entity")
  (query-many [this opts]
    "Queries many entities")
  (create! [this data opts]
    "Creates a new entity"))

(defprotocol IFileAccessor
  "Abstraction for saving and querying files and artifacts"
  (create-artifact! [this artifact opts]
    "Save an artifact to the repository and upload the content to the kv store.
     If write to kv store fails, repository will be rolled back. Otherwise, cleanup TBD")
  (create-file! [this project-id file opts]
    "Save a new file with a version to the repository.")
  (create-file-version! [this file-id version opts]
    "Create a new version of an existing file"))
