(ns com.ben-allred.audiophile.common.infrastructure.resources.impl
  (:require
    [com.ben-allred.audiophile.common.app.navigation.core :as nav]
    [com.ben-allred.audiophile.common.core.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.infrastructure.http.core :as http]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(deftype ArtifactResource [http-client nav]
  pres/IResource
  (request! [_ opts]
    (http/get http-client
              (nav/path-for nav :api/artifact {:route-params opts})
              {:response-type :blob})))

(defn res-artifact [{:keys [http-client nav]}]
  (->ArtifactResource http-client nav))
