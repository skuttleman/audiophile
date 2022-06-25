(ns audiophile.backend.infrastructure.pubsub.handlers.users
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.core.serdes.jwt :as jwt]
    [audiophile.backend.infrastructure.pubsub.handlers.common :as hc]
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
  {:user/id (q/insert-user! executor user opts)})

(defmethod wf/command-handler :user/create!
  [executor {:keys [commands events]} {command-id :command/id :command/keys [ctx data type]}]
  (log/with-ctx :CP
    (log/info "saving user to db" command-id)
    (hc/with-command-failed! [events type ctx]
      (let [result {:spigot/id     (:spigot/id data)
                    :spigot/result (create* executor (:spigot/params data) ctx)}]
        (ps/emit-command! commands :workflow/next! result ctx)))))

(defmethod wf/command-handler :user/generate-token!
  [_ {:keys [commands events jwt-serde]} {command-id :command/id :command/keys [ctx data]}]
  (log/with-ctx :CP
    (log/info "generating login token" command-id)
    (hc/with-command-failed! [events type ctx]
      (let [result {:login/token (jwt/login-token jwt-serde (:spigot/params data))}]
        (ps/emit-command! commands :workflow/next! (assoc data :spigot/result result) ctx)))))
