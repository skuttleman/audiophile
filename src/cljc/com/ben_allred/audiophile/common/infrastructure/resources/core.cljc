(ns com.ben-allred.audiophile.common.infrastructure.resources.core
  (:require
    [com.ben-allred.audiophile.common.infrastructure.resources.protocols :as pres]))

(defn request!
  ([resource]
   (request! resource nil))
  ([resource opts]
   (pres/request! resource opts)))

(defn status [resource]
  (pres/status resource))

(defn requested? [resource]
  (not= :init (status resource)))

(defn ready? [resource]
  (not= :requesting (status resource)))

(defn success? [resource]
  (= :success (status resource)))

(defn error? [resource]
  (= :error (status resource)))

(defn requesting? [resource]
  (= :requesting (status resource)))
