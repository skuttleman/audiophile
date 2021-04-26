(ns com.ben-allred.audiophile.api.handlers.projects
  (:require
    [com.ben-allred.audiophile.api.services.repositories.projects :as projects]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(defmethod ig/init-key ::fetch-all [_ {:keys [project-repo]}]
  (fn [request]
    (let [user-id (get-in request [:auth/user :data :user :user/id])]
      [::http/ok {:data (projects/query-all project-repo user-id)}])))

(defmethod ig/init-key ::create [_ {:keys [project-repo]}]
  (fn [{project :valid/data :as request}]
    (let [user-id (get-in request [:auth/user :data :user :user/id])]
      [::http/ok {:data (projects/create! project-repo project user-id)}])))
