(ns audiophile.backend.infrastructure.workflows.handlers
  (:require
    [audiophile.backend.core.serdes.jwt :as jwt]
    [audiophile.backend.infrastructure.repositories.comments.queries :as qcomments]
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.backend.infrastructure.repositories.files.queries :as qfiles]
    [audiophile.backend.infrastructure.repositories.projects.queries :as qprojects]
    [audiophile.backend.infrastructure.repositories.teams.queries :as qteams]
    [audiophile.backend.infrastructure.repositories.users.queries :as qusers]
    [audiophile.common.core.utils.logger :as log]))

(defn ^:private create* [executor access? insert! ctx {:spigot/keys [tag params]}]
  (let [ns* (namespace tag)]
    (log/with-ctx :TASK
      (log/info (format "saving %s to db" ns*) ctx)
      (if (access? executor params ctx)
        {(keyword ns* "id") (insert! executor params ctx)}
        (throw (ex-info "insufficient access" params))))))

(defmulti task-handler (fn [_ _ {:spigot/keys [tag]}]
                          tag))

(defmethod task-handler :artifact/create!
  [{:keys [repo]} ctx task]
  (repos/transact! repo
                   create*
                   qfiles/insert-artifact-access?
                   qfiles/insert-artifact!
                   ctx
                   task))

(defmethod task-handler :comment/create!
  [{:keys [repo]} ctx task]
  (repos/transact! repo
                   create*
                   qcomments/insert-comment-access?
                   qcomments/insert-comment!
                   ctx
                   task))

(defmethod task-handler :file/create!
  [{:keys [repo]} ctx task]
  (repos/transact! repo
                   create*
                   qfiles/insert-file-access?
                   qfiles/insert-file!
                   ctx
                   task))

(defmethod task-handler :file-version/create!
  [{:keys [repo]} ctx task]
  (repos/transact! repo
                   create*
                   qfiles/insert-version-access?
                   qfiles/insert-version!
                   ctx
                   task))

(defmethod task-handler :project/create!
  [{:keys [repo]} ctx task]
  (repos/transact! repo
                   create*
                   qprojects/insert-project-access?
                   qprojects/insert-project!
                   ctx
                   task))

(defmethod task-handler :team/create!
  [{:keys [repo]} ctx task]
  (repos/transact! repo
                   create*
                   qteams/insert-team-access?
                   qteams/insert-team!
                   ctx
                   task))

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