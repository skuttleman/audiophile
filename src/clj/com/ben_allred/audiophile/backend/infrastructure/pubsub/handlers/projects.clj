(ns com.ben-allred.audiophile.backend.infrastructure.pubsub.handlers.projects
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.projects.core :as rprojects]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.api.pubsub.core :as ps]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]))

(defn ^:private create* [executor project opts]
  (if (rprojects/insert-project-access? executor project opts)
    (let [project-id (rprojects/insert-project! executor project opts)]
      (rprojects/find-event-project executor project-id))
    (throw (ex-info "insufficient access" project))))

(deftype ProjectCommandHandler [repo ch]
  pint/IMessageHandler
  (handle? [_ msg]
    (= :project/create! (:command/type msg)))
  (handle! [this {command-id :command/id :command/keys [ctx data type]}]
    (log/with-ctx [this :CP]
      (try
        (log/info "saving project to db" command-id)
        (let [project (repos/transact! repo create* data ctx)]
          (ps/emit-event! ch (:project/id project) :project/created project ctx))
        (catch Throwable ex
          (ps/command-failed! ch
                              (or (:request/id ctx)
                                  (uuids/random))
                              (assoc ctx
                                     :error/command type
                                     :error/reason (.getMessage ex)))
          (throw ex))))))

(defn msg-handler [{:keys [ch repo]}]
  (->ProjectCommandHandler repo ch))
