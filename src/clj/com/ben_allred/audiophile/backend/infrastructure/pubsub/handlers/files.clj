(ns com.ben-allred.audiophile.backend.infrastructure.pubsub.handlers.files
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.files.core :as rfiles]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.api.pubsub.core :as ps]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]))

(defn ^:private create-artifact* [executor artifact opts]
  (if (rfiles/insert-artifact-access? executor artifact opts)
    (let [artifact-id (rfiles/insert-artifact! executor artifact opts)]
      (rfiles/find-event-artifact executor artifact-id))
    (throw (ex-info "insufficient access" {}))))

(defn ^:private create-file* [executor file opts]
  (if (rfiles/insert-file-access? executor file opts)
    (let [file-id (rfiles/insert-file! executor file opts)]
      (rfiles/find-event-file executor file-id))
    (throw (ex-info "insufficient access" {}))))

(defn ^:private create-file-version* [executor file-version opts]
  (if (rfiles/insert-version-access? executor file-version opts)
    (let [version-id (rfiles/insert-version! executor file-version opts)]
      (rfiles/find-event-version executor version-id))
    (throw (ex-info "insufficient access" {}))))

(deftype FileCommandHandler [repo ch]
  pint/IMessageHandler
  (handle? [_ msg]
    (contains? #{:artifact/create! :file/create! :file-version/create!} (:command/type msg)))
  (handle! [this {command-id :command/id :command/keys [ctx data type]}]
    (let [[data-type id create*]
          (case type
            :artifact/create! ["artifact" :artifact/id create-artifact*]
            :file/create! ["file" :file/id create-file*]
            :file-version/create! ["file-version" :file-version/id create-file-version*])]
      (log/with-ctx [this :CP]
        (try
          (log/info "saving" data-type "to db" command-id)
          (let [payload (repos/transact! repo create* data ctx)]
            (ps/emit-event! ch (get payload id) (keyword data-type "created") payload ctx))
          (catch Throwable ex
            (ps/command-failed! ch
                                (or (:request/id ctx)
                                    (uuids/random))
                                (assoc ctx
                                       :error/command type
                                       :error/reason (.getMessage ex)))
            (throw ex)))))))

(defn msg-handler [{:keys [ch repo]}]
  (->FileCommandHandler repo ch))
