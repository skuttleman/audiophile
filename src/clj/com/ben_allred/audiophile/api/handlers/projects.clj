(ns com.ben-allred.audiophile.api.handlers.projects
  (:require
    [com.ben-allred.audiophile.api.services.interactors.core :as int]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(defmethod ig/init-key ::fetch-all [_ {:keys [interactor]}]
  (fn [request]
    (let [user-id (get-in request [:auth/user :user/id])]
      [::http/ok {:data (int/query-many interactor {:user/id user-id})}])))

(defmethod ig/init-key ::fetch [_ {:keys [interactor]}]
  (fn [request]
    (let [project-id (get-in request [:nav/route :route-params :project-id])
          user-id (get-in request [:auth/user :user/id])]
      [::http/ok {:data (int/query-one interactor {:user/id    user-id
                                                   :project/id project-id})}])))

(defmethod ig/init-key ::create [_ {:keys [interactor]}]
  (fn [{project :valid/data :as request}]
    (let [user-id (get-in request [:auth/user :user/id])]
      [::http/ok {:data (int/create! interactor project {:user/id user-id})}])))
