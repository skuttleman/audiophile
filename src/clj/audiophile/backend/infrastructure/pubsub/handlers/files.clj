(ns audiophile.backend.infrastructure.pubsub.handlers.files
  (:require
    [audiophile.backend.infrastructure.repositories.files.queries :as q]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.logger :as log]))

(defn ^:private create-artifact* [executor artifact opts]
  (if (q/insert-artifact-access? executor artifact opts)
    (q/insert-artifact! executor artifact opts)
    (throw (ex-info "insufficient access" {}))))

(defn ^:private create-file* [executor file opts]
  (if (q/insert-file-access? executor file opts)
    (q/insert-file! executor file opts)
    (throw (ex-info "insufficient access" {}))))

(defn ^:private create-file-version* [executor file-version opts]
  (if (q/insert-version-access? executor file-version opts)
    (q/insert-version! executor file-version opts)
    (throw (ex-info "insufficient access" {}))))

(defn ^:private handler* [create* executor msg]
  (let [{command-id :command/id :command/keys [ctx data type]} msg]
    (log/info "saving " (namespace type) " to db" command-id)
    (create* executor (:spigot/params data) ctx)))

(defmethod wf/command-handler :artifact/create!
  [executor _sys msg]
  {:artifact/id (handler* create-artifact* executor msg)})

(defmethod wf/command-handler :file/create!
  [executor _sys msg]
  {:file/id (handler* create-file* executor msg)})

(defmethod wf/command-handler :file-version/create!
  [executor _sys msg]
  {:file-version/id (handler* create-file-version* executor msg)})
