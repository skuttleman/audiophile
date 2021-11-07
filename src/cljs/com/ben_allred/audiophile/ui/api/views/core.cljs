(ns com.ben-allred.audiophile.ui.api.views.core
  (:require
    [com.ben-allred.audiophile.common.api.navigation.core :as nav]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.ui.api.views.protocols :as vp]
    [com.ben-allred.audiophile.ui.core.components.core :as comp]
    [com.ben-allred.audiophile.ui.core.components.input-fields :as in]))

(defmethod comp/resource-error ::profile
  [_ _ opts]
  [in/spinner {:size (:spinner/size opts)}])

(defn ^:private root* [_ components-table state]
  (let [handle (get-in state [:nav/route :handle])
        component (get components-table handle comp/not-found)]
    [component state]))

(defn ^:private with-auth [nav components-table state *profile]
  (let [{user :auth/user route :nav/route} state]
    (cond
      (= (:token/type user) :token/signup)
      [root* nil components-table (assoc-in state [:nav/route :handle] :ui/signup)]

      (= :ui/login (:handle route))
      [root* nil components-table state]

      user
      [comp/with-resource [*profile {::comp/error-type ::profile}] root* components-table state]

      :else
      (nav/navigate! nav :ui/login {:params {:redirect-uri (:path route)}}))))

(defn root [{:keys [*profile banners components-table header modals nav toasts]}]
  (fn [state]
    [:div
     [banners (:banners state)]
     [header state]
     [:div.main.layout--inset
      {:class [(str "page-" (some-> state (get-in [:nav/route :handle]) name))]}
      [:div.layout--inset
       [with-auth nav components-table state *profile]]]
     [modals (:modals state)]
     [toasts (:toasts state)]]))

(defn comment-form [interactor file-id file-version-id]
  (vp/comment-form interactor file-id file-version-id))

(defn on-comment-created
  ([interactor]
   (on-comment-created interactor (constantly nil)))
  ([interactor cb]
   (vp/on-comment-created interactor cb)))

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

(defn signup-form [interactor]
  (vp/signup-form interactor))

(defn field-resource [interactor path]
  (vp/field-resource interactor path))

(defn on-blur [interactor path]
  (vp/on-blur interactor path))

(defn on-user-created [interactor cb]
  (vp/on-user-created interactor cb))

(defn qp-form [interactor file-version-id]
  (vp/qp-form interactor file-version-id))

(defn update-qp! [interactor m]
  (vp/update-qp! interactor m))
