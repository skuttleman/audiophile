(ns com.ben-allred.audiophile.common.views.roots.login
  (:require
    [com.ben-allred.audiophile.common.services.navigation :as nav]
    [com.ben-allred.audiophile.common.services.ui-store.core :as ui-store]
    [com.ben-allred.audiophile.common.utils.dom :as dom]
    [integrant.core :as ig]))

(defn root* [nav state]
  (fn [nav state]
    [:div
     [:button.button {:on-click (fn [_]
                                  (.assign (.-location dom/window)
                                           (nav/path-for nav
                                                         :auth/login
                                                         {:query-params {:email        "skuttleman@gmail.com"
                                                                         :redirect-uri "/"}})))}
      "login"]]))

(defmethod ig/init-key ::root [_ {:keys [nav store]}]
  (fn [state]
    (if (:auth/user state)
      (ui-store/dispatch! store [:router/navigate! :ui/home])
      [root* nav state])))
