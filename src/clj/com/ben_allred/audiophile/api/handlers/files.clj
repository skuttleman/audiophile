(ns com.ben-allred.audiophile.api.handlers.files
  (:require
    [com.ben-allred.audiophile.api.services.repositories.files.model :as files]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(defmethod ig/init-key ::upload [_ {:keys [repo]}]
  (fn [request]
    (let [artifact-data (colls/only! (:valid/data request))
          user-id (get-in request [:auth/user :data :user :user/id])
          artifact (files/create-artifact repo artifact-data user-id)]
      [::http/ok {:data artifact}])))

(defmethod ig/init-key ::fetch-all [_ {:keys [repo]}]
  (fn [request]
    (let [project-id (get-in request [:nav/route :route-params :project-id])
          user-id (get-in request [:auth/user :data :user :user/id])]
      [::http/ok {:data (files/query-many repo project-id user-id)}])))

(defmethod ig/init-key ::create [_ {:keys [repo]}]
  (fn [request]
    (let [project-id (get-in request [:nav/route :route-params :project-id])
          user-id (get-in request [:auth/user :data :user :user/id])
          file (:valid/data request)]
      [::http/ok (files/create-file repo project-id file user-id)])))

(defmethod ig/init-key ::create-version [_ {:keys [repo]}]
  (fn [request]
    (let [{:keys [file-id]} (get-in request [:nav/route :route-params])
          user-id (get-in request [:auth/user :data :user :user/id])
          version (:valid/data request)]
      [::http/ok (files/create-file-version repo file-id version user-id)])))

(defmethod ig/init-key ::download [_ {:keys [repo]}]
  (constantly [::http/not-implemented]))
