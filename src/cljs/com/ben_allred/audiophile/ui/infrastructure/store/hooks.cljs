(ns com.ben-allred.audiophile.ui.infrastructure.store.hooks
  (:require
    [com.ben-allred.audiophile.ui.core.forms.protocols :as pforms]
    [com.ben-allred.audiophile.common.app.navigation.core :as nav]
    [com.ben-allred.audiophile.common.app.navigation.protocols :as pnav]
    [com.ben-allred.audiophile.ui.core.components.core :as comp]
    [com.ben-allred.audiophile.ui.core.components.protocols :as pcomp]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.ui.infrastructure.store.actions :as actions]
    [com.ben-allred.audiophile.ui.infrastructure.store.core :as store]))

(deftype NavigationTracker [*banners nav store]
  pnav/ITrackNavigation
  (on-change [_ route]
    (if-let [err (keyword (get-in route [:query-params :error-msg]))]
      (comp/create! *banners :error err)
      (store/dispatch! store [:router/updated route]))))

(defn tracker [{:keys [*banners nav store]}]
  (->NavigationTracker *banners nav store))

(deftype Toaster [store]
  pcomp/IAlert
  (create! [_ level body]
    (log/warn "TOAST")
    (store/dispatch! store (actions/toast! level body)))
  (remove! [_ id]
    (store/dispatch! store (actions/remove-toast! id))))

(defn toasts [{:keys [store]}]
  (->Toaster store))

(deftype Banner [store]
  pcomp/IAlert
  (create! [_ level body]
    (store/dispatch! store (actions/banner! level body)))
  (remove! [_ id]
    (store/dispatch! store (actions/remove-banner! id))))

(defn banners [{:keys [store]}]
  (->Banner store))

(deftype Modals [store]
  pcomp/IModal
  (modal! [_ header body buttons]
    (store/dispatch! store (actions/modal! header body buttons)))
  (remove-one! [_ id]
    (store/dispatch! store (actions/remove-modal! id)))
  (remove-all! [_]
    (store/dispatch! store actions/remove-modal-all!)))

(defn modals [{:keys [store]}]
  (->Modals store))

(deftype RouteLink [store nav]
  pforms/ILinkRoute
  (update-qp! [_ f]
    (let [{:keys [handle] :as params} (:nav/route (store/get-state store))]
      (nav/replace! nav handle (update params :query-params f)))))

(defn route-link [{:keys [nav store]}]
  (->RouteLink store nav))
