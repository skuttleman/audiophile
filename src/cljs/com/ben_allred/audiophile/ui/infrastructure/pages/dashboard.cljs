(ns com.ben-allred.audiophile.ui.infrastructure.pages.dashboard
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.ui.infrastructure.components.projects :as cproj]
    [com.ben-allred.audiophile.ui.infrastructure.components.teams :as cteams]
    [com.ben-allred.audiophile.ui.infrastructure.services.teams :as steams]
    [reagent.core :as r]))

(defn ^:private page* [sys _state]
  (r/with-let [*teams (steams/res:fetch-all sys)]
    [:div.level.layout--space-below.layout--xxl.gutters
     {:style {:align-items :flex-start}}
     [cproj/tile sys *teams]
     [cteams/tile sys *teams]]))

(defn page [sys state]
  [page* sys state])
