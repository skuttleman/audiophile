(ns com.ben-allred.audiophile.api.handlers.projects
  (:require
    [com.ben-allred.audiophile.api.services.interactors.projects :as projects]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(defmethod ig/init-key ::fetch-all [_ {:keys [repo]}]
  (fn [request]
    (let [user-id (get-in request [:auth/user :user/id])]
      [::http/ok {:data (projects/query-all repo user-id)}])))

(defmethod ig/init-key ::fetch [_ {:keys [repo]}]
  (fn [request]
    (let [project-id (get-in request [:nav/route :route-params :project-id])
          user-id (get-in request [:auth/user :user/id])]
      [::http/ok {:data (projects/query-by-id repo project-id user-id)}])))

(defmethod ig/init-key ::create [_ {:keys [repo]}]
  (fn [{project :valid/data :as request}]
    (let [user-id (get-in request [:auth/user :user/id])]
      [::http/ok {:data (projects/create! repo project user-id)}])))
