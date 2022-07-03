(ns audiophile.backend.infrastructure.pubsub.handlers.users
  (:require
    [audiophile.backend.core.serdes.jwt :as jwt]
    [audiophile.backend.infrastructure.repositories.users.queries :as qusers]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.logger :as log]))

(defn ^:private query-signup-conflicts [executor user opts]
  (for [[field f] [[:user/email qusers/find-by-email]
                   [:user/handle qusers/find-by-handle]
                   [:user/mobile-number qusers/find-by-mobile-number]]
        :let [val (get user field)]
        :when (and val (f executor val opts))]
    [field val]))

(defn ^:private create* [executor user opts]
  (when-let [fields (some->> (query-signup-conflicts executor user opts)
                             seq
                             (into {}))]
    (throw (ex-info "one or more unique fields are present" {:conflicts fields})))
  (qusers/insert-user! executor user opts))

(wf/defhandler user/create!
  [executor _sys {command-id :command/id :command/keys [ctx data]}]
  (log/info "saving user to db" command-id)
  {:user/id (create* executor data ctx)})

(wf/defhandler user/generate-token!
  [_executor {:keys [jwt-serde]} {command-id :command/id :command/keys [data]}]
  (log/info "generating login token" command-id)
  {:login/token (jwt/login-token jwt-serde data)})
