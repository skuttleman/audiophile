(ns com.ben-allred.audiophile.ui.core.components.protocols)

(defprotocol IAlert
  "Handles the generation and removal of in-app alerts"
  (create! [this opts]
    "Create a new alert. Returns a unique id for the alert.")
  (remove! [this id]
    "Removes an alert by id."))

(defprotocol IMultiAlert
  "Handles adding/removing modals from the page"
  (remove-all! [this]
    "Removes all modals."))
