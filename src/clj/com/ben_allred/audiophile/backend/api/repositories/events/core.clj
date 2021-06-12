(ns com.ben-allred.audiophile.backend.api.repositories.events.core
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.events.protocols :as pe]))

(defn insert-event!
  ([executor event]
   (insert-event! executor event nil))
  ([executor event opts]
   (pe/insert-event! executor event opts)))
