(ns com.ben-allred.audiophile.api.handlers.teams
  (:require
    [com.ben-allred.audiophile.api.services.repositories.teams.model :as teams]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(defmethod ig/init-key ::fetch-all [_ {:keys [repo]}]
  (fn [request]
    (let [user-id (get-in (log/spy-tap :auth/user request) [:auth/user :user/id])]
      [::http/ok {:data (teams/query-all repo user-id)}])))

(defmethod ig/init-key ::fetch [_ {:keys [repo]}]
  (fn [request]
    (let [user-id (get-in (log/spy-tap :auth/user request) [:auth/user :user/id])
          team-id (get-in request [:nav/route :route-params :team-id])]
      [::http/ok {:data (teams/query-by-id repo team-id user-id)}])))

(defmethod ig/init-key ::create [_ {:keys [repo]}]
  (fn [{team :valid/data :as request}]
    (let [user-id (get-in (log/spy-tap :auth/user request) [:auth/user :user/id])]
      [::http/ok {:data (teams/create! repo team user-id)}])))
