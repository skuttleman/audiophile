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

(defprotocol IIdentify
  "Abstraction for a self identifying entity."
  (id [this] "Returns the id"))

(defprotocol ISelectRegion
  "Abstraction for marking an entity by region."
  (set-region! [this opts]
    "Sets the region details.")
  (region [this]
     "Returns the region details."))

(defprotocol ILoad
  "Loadable resource"
  (load! [this opts]
    "Loads the resource")
  (ready? [this]
    "Is the resource ready for use.")
  (destroy! [this]
    "Destroy the resource."))

(defprotocol IPlayer
  "Interactable player"
  (play-pause! [this]
    "Play or pause the player.")
  (playing? [this]
    "Is the resource playing."))
