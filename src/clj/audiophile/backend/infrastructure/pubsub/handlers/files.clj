(ns audiophile.backend.infrastructure.pubsub.handlers.files
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.backend.infrastructure.repositories.files.queries :as q]
    [audiophile.backend.domain.interactors.protocols :as pint]
    [audiophile.backend.infrastructure.pubsub.handlers.common :as hc]
    [audiophile.common.core.utils.logger :as log]))

(defn ^:private create-artifact* [executor artifact opts]
  (if (q/insert-artifact-access? executor artifact opts)
    (let [artifact-id (q/insert-artifact! executor artifact opts)]
      (q/find-event-artifact executor artifact-id))
    (throw (ex-info "insufficient access" {}))))

(defn ^:private create-file* [executor file opts]
  (if (q/insert-file-access? executor file opts)
    (let [file-id (q/insert-file! executor file opts)]
      (q/find-event-file executor file-id))
    (throw (ex-info "insufficient access" {}))))

(defn ^:private create-file-version* [executor file-version opts]
  (if (q/insert-version-access? executor file-version opts)
    (let [version-id (q/insert-version! executor file-version opts)]
      (q/find-event-version executor version-id))
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
