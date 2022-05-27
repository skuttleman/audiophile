(ns audiophile.ui.views.dashboard.core
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.ui.views.dashboard.projects :as cproj]
    [audiophile.ui.views.dashboard.services :as serv]
    [audiophile.ui.views.dashboard.teams :as cteams]
    [reagent.core :as r]))

(defn ^:private page [sys _state]
  (r/with-let [*teams (serv/teams#res:fetch-all sys)]
    [:div.level.layout--space-below.layout--xxl.gutters
     {:style {:align-items :flex-start}}
     [cproj/tile sys *teams]
     [cteams/tile sys *teams]]
    (finally
      (res/destroy! *teams))))

(defn root [sys state]
  [page sys state])
