(ns com.ben-allred.audiophile.common.views.roots.home
  (:require
    [com.ben-allred.audiophile.common.services.navigation :as nav]
    [com.ben-allred.audiophile.common.utils.dom :as dom]
    [integrant.core :as ig]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [clojure.pprint :as pp]))

(defmethod ig/init-key ::root [_ {:keys [nav]}]
  (fn [state]
    (pp/pprint nav)
    (if-not (log/spy :warn (:auth/user state))
      (nav/navigate! nav :ui/login)
      [:div "home"
       [:button.button {:on-click (fn [_]
                                    (.assign (.-location dom/window)
                                             (nav/path-for nav :auth/logout)))}
        "logout"]])))
