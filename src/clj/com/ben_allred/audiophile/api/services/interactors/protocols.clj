(ns com.ben-allred.audiophile.api.services.interactors.protocols)

(defprotocol IAccessor
  "Abstraction for interacting with a repository"
  (query-one [this opts]
    "Queries one entity")
  (query-many [this opts]
    "Queries many entities")
  (create! [this opts]
    "Creates a new entity"))

(defprotocol IFileAccessor
  "Abstraction for saving and querying files and artifacts"
  (create-artifact! [this opts]
    "Create an artifact in the repository and upload the content to a kv store.")
  (create-file! [this opts]
    "Create a new file with a version to the repository.")
  (create-file-version! [this opts]
    "Create a new version of an existing file")
  (get-artifact [this opts]
    "Get artifact data from a kv store"))

(defprotocol IProjectAccessor
  "Abstraction for saving and querying projects")

(defprotocol ITeamAccessor
  "Abstraction for saving and querying teams and user/team relationships")

(defprotocol IUserAccessor
  "Abstraction for saving and querying users")
