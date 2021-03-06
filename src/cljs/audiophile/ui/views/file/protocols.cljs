(ns audiophile.ui.views.file.protocols)

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
  (error? [this]
          "Is the resource in an error state."))

(defprotocol IPlayer
  "Interact-able player"
  (play-pause! [this]
               "Play or pause the player.")
  (playing? [this]
            "Is the resource playing."))
