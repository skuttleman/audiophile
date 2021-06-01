(ns com.ben-allred.audiophile.common.infrastructure.views.core
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.infrastructure.views.components.core :as comp]))

(defn ^:private root* [components-table state]
  (let [handle (get-in state [:nav/route :handle])
        component (get components-table handle comp/not-found)]
    [component state]))

(defn root [{:keys [banners components-table header modals toasts]}]
  (fn [state]
    [:div
     [banners (:banners state)]
     [header state]
     [:div.main.layout--inset
      {:class [(str "page-" (some-> state (get-in [:nav/route :handle]) name))]}
      [:div.layout--inset
       [root* components-table state]]]
     [modals (:modals state)]
     [toasts (:toasts state)]]))
