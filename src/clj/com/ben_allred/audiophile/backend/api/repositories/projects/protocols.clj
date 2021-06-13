(ns com.ben-allred.audiophile.backend.api.repositories.projects.protocols)

(defprotocol IProjectsExecutor
  "Abstraction for querying projects and their relations"
  (find-by-project-id [this project-id opts]
    "Returns a project with the id of `project-id`. Optionally limits search results
     by (:user/id opts) when included.")
  (select-for-user [this user-id opts]
    "Returns all projects for which the `user-id` is a member.")
  (insert-project! [this project opts]
    "Inserts a project and user/project relation. Returns primary id for new project")
  (find-event-project [this project-id]
    "Finds event-ready project by id."))

(defprotocol IProjectsEventEmitter
  "Abstraction for emitting events related to projects"
  (project-created! [this user-id project ctx]
    "Emitted when an project is created in the system"))
