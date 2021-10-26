(ns com.ben-allred.audiophile.backend.api.repositories.search.protocols)

(defprotocol ISearchUserExecutor
  "Abstraction over querying things from a repository related to users and their relations"
  (find-by-handle [this handle opts]
    "Find a user by handle.")
  (find-by-mobile-number [this mobile-number opts]
    "Find a user by mobile-number."))
