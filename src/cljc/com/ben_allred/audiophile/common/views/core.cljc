(ns com.ben-allred.audiophile.common.views.core
  (:require
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.views.components.core :as comp]
    [integrant.core :as ig]))

(defn root [components-table state]
  (let [handler (get-in state [:page :handler])
        component (get components-table handler comp/not-found)]
    [component state]))

(defmethod ig/init-key ::app [_ {:keys [banners components-table header modals toasts]}]
  (fn [state]
    [:div
     [banners (:banners state)]
     [header state]
     [:div.main.layout--inset
      {:class [(str "page-" (some-> state (get-in [:page :handler]) name))]}
      [:div.layout--inset
       [root components-table state]]]
     [modals (:modals state)]
     [toasts (:toasts state)]]))
