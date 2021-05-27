(ns com.ben-allred.audiophile.api.services.repositories.users.protocols)

(defprotocol IUserExecutor
  "Abstraction over querying things from a sql db related to the users table and its relations"
  (find-by-email [this email opts]
    "Find a user by email."))
