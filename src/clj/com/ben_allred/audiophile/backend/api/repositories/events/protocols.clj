(ns com.ben-allred.audiophile.backend.api.repositories.events.protocols)

(defprotocol IEventsExecutor
  "Abstraction for querying events"
  (insert-event! [this event opts]
    "Inserts a new event. Returns primary id for new event")
  (select-for-user [this user-id opts]
    "Queries events for a user id."))
