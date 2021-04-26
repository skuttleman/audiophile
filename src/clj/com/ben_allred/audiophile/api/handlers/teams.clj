(ns com.ben-allred.audiophile.api.handlers.teams
  (:require
    [com.ben-allred.audiophile.api.services.repositories.teams :as teams]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(defmethod ig/init-key ::fetch-all [_ {:keys [team-repo]}]
  (fn [request]
    (let [user-id (get-in request [:auth/user :data :user :user/id])]
      [::http/ok {:data (teams/query-all team-repo user-id)}])))

(defmethod ig/init-key ::create [_ {:keys [team-repo]}]
  (fn [{team :valid/data :as request}]
    (let [user-id (get-in request [:auth/user :data :user :user/id])]
      [::http/ok {:data (teams/create! team-repo team user-id)}])))
