(ns audiophile.backend.infrastructure.workflows.handlers
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.core.serdes.jwt :as jwt]
    [audiophile.backend.infrastructure.repositories.comments.queries :as qcomments]
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.backend.infrastructure.repositories.files.queries :as qfiles]
    [audiophile.backend.infrastructure.repositories.projects.queries :as qprojects]
    [audiophile.backend.infrastructure.repositories.team-invitations.queries :as qinvitations]
    [audiophile.backend.infrastructure.repositories.teams.queries :as qteams]
    [audiophile.backend.infrastructure.repositories.users.queries :as qusers]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.uuids :as uuids]))

(defn ^:private create* [executor insert! ctx {:spigot/keys [tag params]}]
  (let [ns* (namespace tag)]
    (log/with-ctx :TASK
      (log/info (format "saving %s to db" ns*) ctx)
      {(keyword ns* "id") (insert! executor params ctx)})))

(defmulti task-handler (fn [_ _ {:spigot/keys [tag]}]
                          tag))

(defmethod task-handler :artifact/create!
  [{:keys [repo]} ctx task]
  (repos/transact! repo create* qfiles/insert-artifact! ctx task))

(defmethod task-handler :comment/create!
  [{:keys [repo]} ctx task]
  (repos/transact! repo create* qcomments/insert-comment! ctx task))

(defmethod task-handler :file/create!
  [{:keys [repo]} ctx task]
  (repos/transact! repo create* qfiles/insert-file! ctx task))

(defmethod task-handler :file-version/create!
  [{:keys [repo]} ctx task]
  (repos/transact! repo create* qfiles/insert-version! ctx task))

(defmethod task-handler :project/create!
  [{:keys [repo]} ctx task]
  (repos/transact! repo create* qprojects/insert-project! ctx task))

(defmethod task-handler :project/update!
  [{:keys [repo]} ctx {:spigot/keys [params]}]
  (repos/transact! repo qprojects/update-project! params ctx))

(defmethod task-handler :pubsub/publish!
  [{:keys [pubsub]} ctx {{:keys [events]} :spigot/params}]
  (doseq [{:keys [topic payload]} events
          :let [event-id (uuids/random)]]
    (ps/publish! pubsub topic event-id payload ctx)))

(defmethod task-handler :team-invitation/create!
  [{:keys [repo]} ctx {:spigot/keys [params]}]
  (repos/transact! repo qinvitations/invite-member! params ctx))

(defmethod task-handler :team-invitation/update!
  [{:keys [repo]} ctx {:spigot/keys [params]}]
  (repos/transact! repo qinvitations/update-invitation! params ctx))

(defmethod task-handler :team/create!
  [{:keys [repo]} ctx task]
  (repos/transact! repo create* qteams/insert-team! ctx task))

(defmethod task-handler :team/update!
  [{:keys [repo]} ctx {:spigot/keys [params]}]
  (repos/transact! repo qteams/update-team! params ctx))

(defn ^:private query-signup-conflicts [executor params ctx]
  (for [[field f] [[:user/email qusers/find-by-email]
                   [:user/handle qusers/find-by-handle]
                   [:user/mobile-number qusers/find-by-mobile-number]]
        :let [val (get params field)]
        :when (and val (f executor val ctx))]
    [field val]))

(defn ^:private create-user* [executor params ctx]
  (when-let [fields (some->> (query-signup-conflicts executor params ctx)
                             seq
                             (into {}))]
    (throw (ex-info "one or more unique fields are present" {:conflicts fields})))
  (qusers/insert-user! executor params ctx))

(defmethod task-handler :user/create!
  [{:keys [repo]} ctx {:spigot/keys [params]}]
  (log/info "saving user to db" ctx)
  {:user/id (repos/transact! repo create-user* params ctx)})

(defmethod task-handler :user/generate-token!
  [{:keys [jwt-serde]} ctx {:spigot/keys [params]}]
  (log/info "generating login token" ctx)
  {:login/token (jwt/login-token jwt-serde params)})
