(ns com.ben-allred.audiophile.ui.api.views.core
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.ui.api.views.protocols :as vp]
    [com.ben-allred.audiophile.ui.core.components.core :as comp]))

(defn ^:private root* [components-table state]
  (let [handle (get-in state [:nav/route :handle])
        component (get components-table handle comp/not-found)]
    [component state]))

(defn project-form [interactor team-options]
  (vp/project-form interactor team-options))

(defn on-project-created [interactor cb]
  (vp/on-project-created interactor cb))

(defn file-form [interactor project-id]
  (vp/file-form interactor project-id))

(defn on-file-created [interactor project-id cb]
  (vp/on-file-created interactor project-id cb))

(defn version-form [interactor project-id file-id]
  (vp/version-form interactor project-id file-id))

(defn on-version-created [interactor project-id cb]
  (vp/on-version-created interactor project-id cb))

(defn team-form [interactor]
  (vp/team-form interactor))

(defn on-team-created [interactor cb]
  (vp/on-team-created interactor cb))

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
