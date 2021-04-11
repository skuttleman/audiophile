(ns com.ben-allred.audiophile.api.dev.handler
  (:require
    [integrant.core :as ig]))

(defmethod ig/init-key ::app [_ {:keys [app]}]
  app)
