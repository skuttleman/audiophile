(ns com.ben-allred.audiophile.common.core.ui-components.protocols)

(defprotocol IAlert
  "Handles the generation and removal of in-app alerts"
  (create! [this level body]
    "Create a new alert. Returns a unique id for the alert.")
  (remove! [this id]
    "Removes an alert by id."))

(defprotocol IModal
  "Handles adding/removing modals from the page"
  (modal! [this header body buttons]
    "Create a new modal. Returns a unique id for the modal")
  (remove-one! [this id]
    "Removes one modal by id.")
  (remove-all! [this]
    "Removes all modals."))
