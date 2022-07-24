(ns audiophile.ui.services.login
  (:require
    [audiophile.common.infrastructure.navigation.core :as nav]
    [reagent.core :as r]))

(defmulti form (fn [type _ _]
                 type))

(defn nav#logout! [{:keys [nav]}]
  (fn [_]
    (nav/goto! nav :routes.auth/logout)))

(defn logout [{:keys [text] :as attrs}]
  (r/with-let [logout (nav#logout! attrs)]
    [:a (-> attrs
            (select-keys #{:class})
            (assoc :href "#"
                   :on-click logout)
            (cond-> (not (:minimal? attrs)) (update :class conj "button" "is-primary")))
     (or text "Logout")]))
