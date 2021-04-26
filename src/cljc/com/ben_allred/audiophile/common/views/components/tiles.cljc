(ns com.ben-allred.audiophile.common.views.components.tiles
  (:require
    [com.ben-allred.audiophile.common.views.components.core :as comp]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]))

(defmethod ig/init-key ::with-resource [_ {:keys [links nav resource title]}]
  (fn [state view & args]
    [:div.tile
     [:div.panel {:style {:min-width "400px"}}
      [:div.panel-heading
       [:h2.subtitle title]]
      [:div.panel-tabs
       (for [{:keys [label page params]} links]
         ^{:key [page label]}
         [:a.link {:href (nav/path-for nav page params)} label])]
      [:div.panel-block
       (cond-> [comp/with-resource [resource {:spinner/size :small}] view state]
         (seq args) (into args))]]]))
