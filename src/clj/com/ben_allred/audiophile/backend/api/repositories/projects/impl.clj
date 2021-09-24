(ns com.ben-allred.audiophile.backend.api.repositories.projects.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.projects.core :as rprojects]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.core :as ps]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn ^:private create* [executor project opts]
  (if (rprojects/insert-project-access? executor project opts)
    (let [project-id (rprojects/insert-project! executor project opts)]
      (rprojects/find-event-project executor project-id))
    (throw (ex-info "insufficient access" project))))

(deftype ProjectAccessor [repo commands events]
  pint/IProjectAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo rprojects/select-for-user (:user/id opts) opts))
  (query-one [_ opts]
    (repos/transact! repo rprojects/find-by-project-id (:project/id opts) opts))
  (create! [_ data opts]
    (ps/emit-command! commands (:user/id opts) :project/create! data opts))

  pint/IMessageHandler
  (handle! [_ {[command-id {:command/keys [type] :as command} ctx] :msg :as msg}]
    (when (= type :project/create!)
      (try
        (log/info "saving project to db" command-id)
        (let [project (repos/transact! repo create* (:command/data command) ctx)]
          (ps/emit-event! events (:user/id ctx) (:project/id project) :project/created project ctx))
        (catch Throwable ex
          (log/error ex "failed: saving project to db" msg)
          (try
            (ps/command-failed! events
                                (:request/id ctx)
                                (assoc ctx
                                       :error/command (:command/type command)
                                       :error/reason (.getMessage ex)))
            (catch Throwable ex
              (log/error ex "failed to emit command/failed"))))))))

(defn accessor
  "Constructor for [[ProjectAccessor]] which provides semantic access for storing and retrieving projects."
  [{:keys [commands events repo]}]
  (->ProjectAccessor repo commands events))
