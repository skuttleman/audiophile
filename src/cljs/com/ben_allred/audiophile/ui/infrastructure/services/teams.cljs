(ns com.ben-allred.audiophile.ui.infrastructure.services.teams
  (:require
    [com.ben-allred.audiophile.ui.infrastructure.resources.impl :as ires]
    [com.ben-allred.audiophile.common.infrastructure.navigation.core :as nav]))

(defn res:list [{:keys [http-client nav]}]
  (ires/http http-client
             (constantly {:method :get
                          :url    (nav/path-for nav :api/teams)})))
