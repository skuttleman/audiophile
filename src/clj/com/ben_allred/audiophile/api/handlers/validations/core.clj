(ns com.ben-allred.audiophile.api.handlers.validations.core
  (:require
    [com.ben-allred.audiophile.api.handlers.validations.specs :as specs]
    [com.ben-allred.audiophile.api.services.interactors.core :as int]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]
    [spec-tools.core :as st]))

(defn select-input
  "Selects relevant data from HTTP request to be passed to handler"
  [handler request]
  (specs/spec handler request))

(defmethod ig/init-key ::with-spec [_ {:keys [handler spec]}]
  (vary-meta (fn [data]
               (handler (try
                          (st/select-spec spec (st/conform! (log/spy spec) (log/spy data)))
                          (catch Throwable _
                            (int/invalid-input!)))))
             assoc ::embedded? true))
