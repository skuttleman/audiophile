(ns audiophile.backend.infrastructure.repositories.users.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.domain.interactors.protocols :as pint]
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.backend.infrastructure.repositories.users.queries :as q]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.logger :as log]))

(defmethod wf/->ctx :users/signup
  [_]
  '{:user/handle        ?handle
    :user/email         ?email
    :user/first-name    ?first-name
    :user/last-name     ?last-name
    :user/mobile-number ?mobile-number})
(defmethod wf/->result :users/signup
  [_]
  '{:workflows/->result {:login/token (sp.ctx/get ?token)}})

(defmulti ^:private find-by (fn [_ {:user/keys [email id]}]
                              (cond
                                id :user/id
                                email :user/email)))

(defmethod find-by :user/id
  [executor opts]
  (q/find-by-id executor (:user/id opts) opts))

(defmethod find-by :user/email
  [executor opts]
  (q/find-by-email executor (:user/email opts) opts))

(defn ^:private user-accessor#query-one
  [repo {aud :token/aud user :auth/user :as opts}]
  (if (contains? aud :token/signup)
    user
    (repos/transact! repo find-by opts)))

(deftype UserAccessor [repo ch]
  pint/IUserAccessor
  pint/IAccessor
  (query-one [_ opts]
    (user-accessor#query-one repo opts))
  (create! [_ data opts]
    (ps/start-workflow! ch :users/signup data opts)))

(defn accessor
  "Constructor for [[UserAccessor]] which provides semantic access for storing and retrieving users."
  [{:keys [ch repo]}]
  (->UserAccessor repo ch))