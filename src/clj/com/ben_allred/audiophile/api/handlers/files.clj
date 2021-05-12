(ns com.ben-allred.audiophile.api.handlers.files
  (:require
    [com.ben-allred.audiophile.api.services.interactors.core :as int]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(defmethod ig/init-key ::upload [_ {:keys [interactor]}]
  (fn [request]
    (let [artifact-data (colls/only! (:valid/data request))
          user-id (get-in request [:auth/user :user/id])]
      [::http/ok {:data (int/create-artifact! interactor artifact-data {:user/id user-id})}])))

(defmethod ig/init-key ::fetch-all [_ {:keys [interactor]}]
  (fn [request]
    (let [project-id (get-in request [:nav/route :route-params :project-id])
          user-id (get-in request [:auth/user :user/id])]
      [::http/ok {:data (int/query-one interactor {:user/id    user-id
                                                   :project/id project-id})}])))

(defmethod ig/init-key ::create [_ {:keys [interactor]}]
  (fn [request]
    (let [project-id (get-in request [:nav/route :route-params :project-id])
          user-id (get-in request [:auth/user :user/id])
          file (:valid/data request)]
      [::http/ok (int/create-file! interactor project-id file {:user/id user-id})])))

(defmethod ig/init-key ::create-version [_ {:keys [interactor]}]
  (fn [request]
    (let [{:keys [file-id]} (get-in request [:nav/route :route-params])
          user-id (get-in request [:auth/user :user/id])
          version (:valid/data request)]
      [::http/ok (int/create-file-version! interactor file-id version {:user/id user-id})])))

(defmethod ig/init-key ::download [_ {:keys [interactor]}]
  (constantly [::http/not-implemented]))
