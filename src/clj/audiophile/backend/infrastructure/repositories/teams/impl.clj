(ns audiophile.backend.infrastructure.repositories.teams.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.domain.interactors.protocols :as pint]
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.backend.infrastructure.repositories.teams.queries :as q]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.logger :as log]))

(defmethod wf/->ctx :teams/create
  [_]
  '{:team/name ?team-name
    :team/type ?team-type
    :user/id   ?user-id})
(defmethod wf/->result :teams/create
  [_]
  '{:workflows/->result {:team/id (sp.ctx/get ?team-id)}})

(defn ^:private query-by-id* [executor team-id opts]
  (when-let [team (q/find-by-team-id executor team-id opts)]
    (assoc team
           :team/members
           (q/select-team-members executor team-id opts))))

(deftype TeamAccessor [repo ch]
  pint/ITeamAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo q/select-for-user (:user/id opts) opts))
  (query-one [_ opts]
    (repos/transact! repo query-by-id* (:team/id opts) opts))
  (create! [_ data opts]
    (ps/start-workflow! ch :teams/create (merge opts data) opts)))

(defn accessor
  "Constructor for [[TeamAccessor]] which provides semantic access for storing and retrieving teams."
  [{:keys [ch repo]}]
  (->TeamAccessor repo ch))