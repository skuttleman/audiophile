(ns com.ben-allred.audiophile.backend.api.repositories.comments.protocols)

(defprotocol ICommentsExecutor
  "Abstraction for querying comments and their relations"
  (select-for-file [this file-id opts]
    "Finds all comments for a file")
  (insert-comment-access? [this comment opts]
    "Pre-flight check for access to create the comment")
  (insert-comment! [this comment opts]
    "Inserts a comment. Returns primary id for new comment.")
  (find-event-comment [this comment-id]
    "Finds event-ready comment by id."))
