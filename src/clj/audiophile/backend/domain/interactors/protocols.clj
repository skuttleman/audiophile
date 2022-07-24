(ns audiophile.backend.domain.interactors.protocols)

(defprotocol IAccessor
  "Abstraction for interacting with a repository"
  (query-one [this opts]
    "Queries one entity")
  (query-many [this opts]
    "Queries many entities")
  (create! [this data opts]
    "Creates a new entity")
  (update! [this data opts]
    "Updates an existing entity"))

(defprotocol IEventAccessor
  "Abstraction for saving and querying events")

(defprotocol IFileAccessor
  "Abstraction for saving and querying files and artifacts"
  (create-artifact! [this data opts]
    "Create a new artifact in the repository.")
  (create-file! [this data opts]
    "Create a new file with a version to the repository.")
  (create-file-version! [this data opts]
    "Create a new version of an existing file")
  (set-version! [this data opts]
    "Sets a file version as the current version")
  (get-artifact [this opts]
    "Get artifact data from a kv store"))

(defprotocol ICommentAccessor
  "Abstraction for saving and querying comments")

(defprotocol IProjectAccessor
  "Abstraction for saving and querying projects")

(defprotocol ITeamAccessor
  "Abstraction for saving and querying teams and user/team relationships")

(defprotocol ITeamInvitationAccessor
  "Abstraction for saving and querying team-invitations")

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
