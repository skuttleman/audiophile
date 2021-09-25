(ns com.ben-allred.audiophile.backend.infrastructure.pubsub.handlers.projects
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

(deftype ProjectCommandHandler [repo pubsub]
  pint/IMessageHandler
  (handle! [_ {[command-id {:command/keys [type] :as command} ctx] :msg :as msg}]
    (when (= type :project/create!)
      (try
        (log/info "saving project to db" command-id)
        (let [project (repos/transact! repo create* (:command/data command) ctx)]
          (ps/emit-event! pubsub (:user/id ctx) (:project/id project) :project/created project ctx))
        (catch Throwable ex
          (log/error ex "failed: saving project to db" msg)
          (try
            (ps/command-failed! pubsub
                                (:request/id ctx)
                                (assoc ctx
                                       :error/command (:command/type command)
                                       :error/reason (.getMessage ex)))
            (catch Throwable ex
              (log/error ex "failed to emit command/failed"))))))))

(defn msg-handler [{:keys [repo pubsub]}]
  (->ProjectCommandHandler repo pubsub))
