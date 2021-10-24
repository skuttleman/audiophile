(ns com.ben-allred.audiophile.backend.api.repositories.users.protocols)

(defprotocol IUserExecutor
  "Abstraction over querying things from a repository related to users and their relations"
  (find-by-email [this email opts]
    "Find a user by email.")
  (insert-user! [this user opts]
    "Create new user"))
