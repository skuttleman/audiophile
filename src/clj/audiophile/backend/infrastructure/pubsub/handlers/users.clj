(ns audiophile.backend.infrastructure.pubsub.handlers.users
  (:require
    [audiophile.backend.core.serdes.jwt :as jwt]
    [audiophile.backend.infrastructure.repositories.users.queries :as q]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.logger :as log]))

(defn ^:private query-signup-conflicts [executor user opts]
  (for [[field f] [[:user/email q/find-by-email]
                   [:user/handle q/find-by-handle]
                   [:user/mobile-number q/find-by-mobile-number]]
        :let [val (get user field)]
        :when (and val (f executor val opts))]
    [field val]))

(defn ^:private create* [executor user opts]
  (when-let [fields (some->> (query-signup-conflicts executor user opts)
                             seq
                             (into {}))]
    (throw (ex-info "one or more unique fields are present" {:conflicts fields})))
  (q/insert-user! executor user opts))

(defmethod wf/command-handler :user/create!
  [executor _sys {command-id :command/id :command/keys [ctx data]}]
  (log/info "saving user to db" command-id)
  {:user/id (create* executor (:spigot/params data) ctx)})

(defmethod wf/command-handler :user/generate-token!
  [_executor {:keys [jwt-serde]} {command-id :command/id :command/keys [data]}]
  (log/info "generating login token" command-id)
  {:login/token (jwt/login-token jwt-serde (:spigot/params data))})
