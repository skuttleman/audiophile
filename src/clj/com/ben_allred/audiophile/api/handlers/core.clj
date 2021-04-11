(ns com.ben-allred.audiophile.api.handlers.core
  (:require
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(defn handler [request]
  (log/debug request)
  {:status 200 :body "{\"some\":\"json\"}" :headers {"X-Foo" "bar"}})

(defmethod ig/init-key ::app [_ _]
  handler)
