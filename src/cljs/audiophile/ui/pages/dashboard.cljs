(ns audiophile.ui.pages.dashboard
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.ui.pages.projects :as cproj]
    [audiophile.ui.pages.teams :as cteams]
    [audiophile.ui.services.teams :as steams]
    [reagent.core :as r]))

(defn ^:private page* [sys _state]
  (r/with-let [*teams (steams/res:fetch-all sys)]
    [:div.level.layout--space-below.layout--xxl.gutters
     {:style {:align-items :flex-start}}
     [cproj/tile sys *teams]
     [cteams/tile sys *teams]]))

(defn page [sys state]
  [page* sys state])
