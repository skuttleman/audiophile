(ns audiophile.backend.infrastructure.pubsub.handlers.files
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.api.repositories.core :as repos]
    [audiophile.backend.api.repositories.files.core :as rfiles]
    [audiophile.backend.domain.interactors.protocols :as pint]
    [audiophile.backend.infrastructure.pubsub.handlers.common :as hc]
    [audiophile.common.core.utils.logger :as log]))

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

(defn ^:private file-command-handler#handle!
  [this repo ch {command-id :command/id :command/keys [ctx data type]}]
  (let [[data-type id create*]
        (case type
          :artifact/create! ["artifact" :artifact/id create-artifact*]
          :file/create! ["file" :file/id create-file*]
          :file-version/create! ["file-version" :file-version/id create-file-version*])]
    (log/with-ctx [this :CP]
      (hc/with-command-failed! [ch type ctx]
        (log/info "saving" data-type "to db" command-id)
        (let [payload (repos/transact! repo create* data ctx)]
          (ps/emit-event! ch (get payload id) (keyword data-type "created") payload ctx))))))

(deftype FileCommandHandler [repo ch]
  pint/IMessageHandler
  (handle? [_ msg]
    (contains? #{:artifact/create! :file/create! :file-version/create!} (:command/type msg)))
  (handle! [this msg]
    (file-command-handler#handle! this repo ch msg)))

(defn msg-handler [{:keys [ch repo]}]
  (->FileCommandHandler repo ch))
