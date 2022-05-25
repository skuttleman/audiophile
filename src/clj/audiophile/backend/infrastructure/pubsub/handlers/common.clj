(ns audiophile.backend.infrastructure.pubsub.handlers.common
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.core.utils.uuids :as uuids]))

(defmacro with-command-failed! [[ch type ctx] & body]
  `(let [ctx# ~ctx]
     (try
       ~@body
       (catch Throwable ex#
         (ps/command-failed! ~ch
                             (or (:request/id ctx#)
                                 (uuids/random))
                             (maps/assoc-maybe ctx#
                                               :error/command ~type
                                               :error/reason (.getMessage ex#)
                                               :error/details (not-empty (ex-data ex#))))
         nil))))
