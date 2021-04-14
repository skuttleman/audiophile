(ns com.ben-allred.audiophile.common.services.resources.users
  (:require
    [com.ben-allred.audiophile.common.services.http :as http]
    [com.ben-allred.audiophile.common.services.navigation :as nav]
    [integrant.core :as ig]
    [clojure.pprint :as pp]))

(defmethod ig/init-key ::details [_ {:keys [http-client nav] :as thing}]
  (pp/pprint thing)
  (partial http/get
           http-client
           (nav/path-for nav :auth/details)))
