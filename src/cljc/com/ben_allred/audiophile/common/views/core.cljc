(ns com.ben-allred.audiophile.common.views.core
  (:require
    [com.ben-allred.audiophile.common.views.components.core :as comp]
    [integrant.core :as ig]))

(defn root [auth-user state components-table]
  (let [handler (get-in state [:page :handler])
        component (get components-table handler comp/not-found)]
    [component (assoc state :auth/user auth-user)]))

(defmethod ig/init-key ::app [_ {:keys [banners components-table header toasts user-resource]}]
  (fn [state]
    [:div
     [banners (:banners state)]
     [header (assoc state :auth/user @user-resource)]
     [:div.main.layout--inset
      {:class [(str "page-" (some-> state (get-in [:page :handler]) name))]}
      [:div.layout--inset
       [comp/with-resource user-resource nil root state components-table]]]
     [toasts (:toasts state)]]))
