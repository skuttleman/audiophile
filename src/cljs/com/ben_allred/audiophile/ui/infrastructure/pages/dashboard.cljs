(ns com.ben-allred.audiophile.ui.infrastructure.pages.dashboard
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.ui.infrastructure.components.projects :as cproj]
    [com.ben-allred.audiophile.ui.infrastructure.components.teams :as cteams]))

(defn ^:private page* [sys _state]
  [:div.level.layout--space-below.layout--xxl.gutters
   {:style {:align-items :flex-start}}
   [cproj/tile sys]
   [cteams/tile sys]])

(defn page [sys state]
  [page* sys state])
