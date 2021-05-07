(ns com.ben-allred.audiophile.common.views.core
  (:require
    [com.ben-allred.audiophile.common.services.resources.core :as res]
    [com.ben-allred.audiophile.common.services.ui-store.core :as ui-store]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.views.components.core :as comp]
    [integrant.core :as ig]))

(defn root [auth-user store _components-table _state]
  (ui-store/dispatch! store [:user-details/received auth-user])
  (fn [_auth-user _store components-table state]
    (let [handler (get-in state [:page :handler])
          component (get components-table handler comp/not-found)]
      [component state])))

(defmethod ig/init-key ::app [_ {:keys [banners components-table header modals store toasts user-resource]}]
  (fn [state]
    [:div
     [banners (:banners state)]
     [header state]
     [:div.main.layout--inset
      {:class [(str "page-" (some-> state (get-in [:page :handler]) name))]}
      [:div.layout--inset
       [comp/with-resource [user-resource {:spinner/size :large}] root store components-table state]]]
     [modals (:modals state)]
     [toasts (:toasts state)]]))
