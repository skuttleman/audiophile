(ns com.ben-allred.audiophile.api.app.repositories.projects.protocols)

(defprotocol IProjectsExecutor
  "Abstraction for querying projects and their relations"
  (find-by-project-id [this project-id opts]
    "Returns a project with the id of `project-id`. Optionally limits search results
     by (:user/id opts) when included.")
  (select-for-user [this user-id opts]
    "Returns all projects for which the `user-id` is a member.")
  (insert-project! [this project opts]
    "Inserts a project and user/project relation. Returns primary id for new project"))
