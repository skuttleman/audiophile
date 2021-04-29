(ns com.ben-allred.audiophile.common.views.core
  (:require
    [com.ben-allred.audiophile.common.views.components.core :as comp]
    [integrant.core :as ig]
    [com.ben-allred.audiophile.common.services.resources.core :as res]))

(defn root [auth-user components-table state]
  (let [handler (get-in state [:page :handler])
        component (get components-table handler comp/not-found)]
    [component (assoc state :auth/user auth-user)]))

(defmethod ig/init-key ::app [_ {:keys [banners components-table header modals toasts user-resource]}]
  (fn [state]
    [:div
     [banners (:banners state)]
     [header (cond-> state
               (res/success? user-resource) (assoc :auth/user @user-resource))]
     [:div.main.layout--inset
      {:class [(str "page-" (some-> state (get-in [:page :handler]) name))]}
      [:div.layout--inset
       [comp/with-resource [user-resource {:spinner/size :large}] root components-table state]]]
     [toasts (:toasts state)]
     [modals (:modals state)]]))
