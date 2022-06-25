(ns audiophile.backend.infrastructure.pubsub.handlers.files
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.infrastructure.pubsub.handlers.common :as hc]
    [audiophile.backend.infrastructure.repositories.files.queries :as q]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.logger :as log]))

(defn ^:private create-artifact* [executor artifact opts]
  (if (q/insert-artifact-access? executor artifact opts)
    {:artifact/id (q/insert-artifact! executor artifact opts)}
    (throw (ex-info "insufficient access" {}))))

(defn ^:private create-file* [executor file opts]
  (if (q/insert-file-access? executor file opts)
    {:file/id (q/insert-file! executor file opts)}
    (throw (ex-info "insufficient access" {}))))

(defn ^:private create-file-version* [executor file-version opts]
  (if (q/insert-version-access? executor file-version opts)
    {:file-version/id (q/insert-version! executor file-version opts)}
    (throw (ex-info "insufficient access" {}))))

(defn ^:private handler* [create* executor {:keys [commands events]} msg]
  (let [{command-id :command/id :command/keys [ctx data type]} msg]
    (log/with-ctx :CP
      (log/info "saving " (namespace type) " to db" command-id)
      (hc/with-command-failed! [events type ctx]
        (let [result {:spigot/id     (:spigot/id data)
                      :spigot/result (create* executor (:spigot/params data) ctx)}]
          (ps/emit-command! commands :workflow/next! result ctx))))))

(defmethod wf/command-handler :artifact/create!
  [executor sys msg]
  (handler* create-artifact* executor sys msg))

(defmethod wf/command-handler :file/create!
  [executor sys msg]
  (handler* create-file* executor sys msg))

(defmethod wf/command-handler :file-version/create!
  [executor sys msg]
  (handler* create-file-version* executor sys msg))
