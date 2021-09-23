(ns com.ben-allred.audiophile.backend.api.repositories.teams.protocols)

(defprotocol ITeamsExecutor
  "Abstraction for querying teams and its relations"
  (find-by-team-id [this team-id opts]
    "Returns a team with the id of `team-id`. Optionally limits search results
     by (:user/id opts) when included.")
  (select-team-members [this team-id opts]
    "Find users as `members` that belong to `team-id`.")
  (select-for-user [this user-id opts]
    "Returns all teams for which the `user-id` is a member.")
  (insert-team-access? [this team opts]
    "Pre-flight check for access to create the team.")
  (insert-team! [this team opts]
    "Inserts a team and user/team relation. Returns primary id for new team")
  (find-event-team [this team-id]
    "Finds event-ready team by id."))
