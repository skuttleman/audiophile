(ns com.ben-allred.audiophile.backend.app.repositories.users.protocols)

(defprotocol IUserExecutor
  "Abstraction over querying things from a repository related to users and their relations"
  (find-by-email [this email opts]
    "Find a user by email."))
