(ns com.ben-allred.audiophile.backend.domain.interactors.protocols)

(defprotocol IAccessor
  "Abstraction for interacting with a repository"
  (query-one [this opts]
    "Queries one entity")
  (query-many [this opts]
    "Queries many entities")
  (create! [this data opts]
    "Creates a new entity"))

(defprotocol IEmitter
  "Abstraction for sending cross-cutting events"
  (command-failed! [this request-id opts]
    "A command to mutate the system failed"))

(defprotocol IFileAccessor
  "Abstraction for saving and querying files and artifacts"
  (create-file! [this data opts]
    "Create a new file with a version to the repository.")
  (create-file-version! [this data opts]
    "Create a new version of an existing file")
  (get-artifact [this opts]
    "Get artifact data from a kv store"))

(defprotocol IProjectAccessor
  "Abstraction for saving and querying projects")

(defprotocol ITeamAccessor
  "Abstraction for saving and querying teams and user/team relationships")

(defprotocol IUserAccessor
  "Abstraction for saving and querying users")

(defprotocol IAuthInteractor
  "Abstraction for providing auth flow"
  (login [this params]
    "Looks up user and redirects to app with or without token depending on whether a user was found")
  (logout [this params]
    "Redirects to app without token")
  (callback [this params]
    "Handles callback from auth provider"))
