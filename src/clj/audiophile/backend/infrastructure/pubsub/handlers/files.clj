(ns audiophile.backend.infrastructure.pubsub.handlers.files
  (:require
    [audiophile.backend.infrastructure.repositories.files.queries :as qfiles]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.logger :as log]))

(defn ^:private create-artifact* [executor artifact opts]
  (if (qfiles/insert-artifact-access? executor artifact opts)
    (qfiles/insert-artifact! executor artifact opts)
    (throw (ex-info "insufficient access" {}))))

(defn ^:private create-file* [executor file opts]
  (if (qfiles/insert-file-access? executor file opts)
    (qfiles/insert-file! executor file opts)
    (throw (ex-info "insufficient access" {}))))

(defn ^:private create-file-version* [executor file-version opts]
  (if (qfiles/insert-version-access? executor file-version opts)
    (qfiles/insert-version! executor file-version opts)
    (throw (ex-info "insufficient access" {}))))

(defn ^:private handler* [create* executor msg]
  (let [{command-id :command/id :command/keys [ctx data type]} msg]
    (log/info "saving " (namespace type) " to db" command-id)
    (create* executor data ctx)))

(wf/defhandler artifact/create!
  [executor _sys msg]
  {:artifact/id (handler* create-artifact* executor msg)})

(wf/defhandler file/create!
  [executor _sys msg]
  {:file/id (handler* create-file* executor msg)})

(wf/defhandler file-version/create!
  [executor _sys msg]
  {:file-version/id (handler* create-file-version* executor msg)})
