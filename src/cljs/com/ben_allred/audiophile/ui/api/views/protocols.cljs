(ns com.ben-allred.audiophile.ui.api.views.protocols)

(defprotocol IProjectsViewInteractor
  "Infrastructural interactions from projects view"
  (project-form [this team-options] "Creates a project form")
  (on-project-created [this cb] "Handle project creation"))

(defprotocol IFilesViewInteractor
  "Infrastructural interactions from files view"
  (file-form [this project-id] "Creates a file form")
  (on-file-created [this project-id cb] "Handle file creation"))

(defprotocol IVersionViewInteractor
  "Infrastructural interactions from file-versions view"
  (version-form [this project-id file-id] "Creates a file-version form")
  (on-version-created [this project-id cb] "Handle file-version creation"))

(defprotocol ITeamsViewInteractor
  "Infrastructural interactions from teams view"
  (team-form [this] "Creates a team form")
  (on-team-created [this cb] "Handle team creation"))

(defprotocol ICommentsViewInteractor
  "Infrastructural interactions from comments view"
  (comment-form [this file-id file-version-id] "Creates a comment form")
  (on-comment-created [this cb] "Handle comment creation"))

(defprotocol ISignupViewInteractor
  "Infrastructural interactions from signup view"
  (signup-form [this] "Creates a new user form")
  (on-user-created [this cb] "Handle user creation"))

(defprotocol IAsyncFieldValidator
  "Infrastructural interactions for asynchronously validating fields"
  (field-resource [this path] "Resource for the field")
  (on-blur [this path] "Blur handler for field"))

(defprotocol IQueryParamsViewInteractor
  "Infrastructural interactions for query params"
  (qp-form [this file-version-id] "Creates a comment form")
  (update-qp! [this m]))
