(ns com.ben-allred.audiophile.common.infrastructure.ui-store.hooks
  (:require
    [com.ben-allred.audiophile.common.app.navigation.protocols :as pnav]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.audiophile.common.infrastructure.ui-store.actions :as actions]
    [com.ben-allred.audiophile.common.infrastructure.ui-store.core :as ui-store]
    [com.ben-allred.audiophile.common.app.resources.protocols :as pres]))

(deftype NavigationTracker [nav store]
  pnav/ITrackNavigation
  (on-change [_ route]
    (let [route' (maps/update-maybe route :query-params dissoc :error-msg)]
      (if-let [err (get-in route [:query-params :error-msg])]
        (ui-store/dispatch! store (actions/server-err! err))
        (ui-store/dispatch! store [:router/updated route'])))))

(defn tracker [{:keys [nav store]}]
  (->NavigationTracker nav store))

(deftype Toaster [store]
  pres/IToaster
  (toast! [_ level body]
    (ui-store/dispatch! store (actions/toast! level body)))
  (remove-toast! [_ id]
    (ui-store/dispatch! store (actions/remove-toast! id))))

(defn toast-fn [{:keys [store]}]
  (->Toaster store))
