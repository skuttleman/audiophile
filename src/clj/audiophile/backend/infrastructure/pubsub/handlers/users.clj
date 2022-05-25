(ns audiophile.backend.infrastructure.pubsub.handlers.users
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.api.repositories.core :as repos]
    [audiophile.backend.api.repositories.search.core :as search]
    [audiophile.backend.api.repositories.users.core :as rusers]
    [audiophile.backend.domain.interactors.protocols :as pint]
    [audiophile.backend.infrastructure.pubsub.handlers.common :as hc]
    [audiophile.common.core.utils.logger :as log]
    [clojure.set :as set]))

(defn ^:private query-signup-conflicts [executor user opts]
  (for [[field f] [[:user/email rusers/find-by-email]
                   [:user/handle search/find-by-handle]
                   [:user/mobile-number search/find-by-mobile-number]]
        :let [val (get user field)]
        :when (and val (f executor val opts))]
    [field val]))

(defn ^:private create* [executor user opts]
  (when-let [fields (some->> (query-signup-conflicts executor user opts)
                             seq
                             (into {}))]
    (throw (ex-info "one or more unique fields are present" {:conflicts fields})))
  {:user/id (rusers/insert-user! executor user opts)})

(deftype UserCommandHandler [repo commands events]
  pint/IMessageHandler
  (handle? [_ msg]
    (= :user/create! (:command/type msg)))
  (handle! [this {command-id :command/id :command/keys [ctx type data] :as command}]
    (log/with-ctx [this :CP]
      (log/info "saving user to db" command-id)
      (when-let [user (hc/with-command-failed! [events type ctx]
                        (repos/transact! repo create* data ctx))]
        (let [ctx (-> command
                      :command/ctx
                      (set/rename-keys {:user/id :signup/id}))
              ctx' (assoc ctx :user/id (:user/id user))]
          (hc/with-command-failed! [events type ctx]
            (ps/emit-command! commands
                              :team/create!
                              {:team/name "My Personal Projects"
                               :team/type :PERSONAL}
                              ctx')
            (ps/emit-event! events (:user/id user) :user/created user ctx')))))))

(defn msg-handler [{:keys [commands events repo]}]
  (->UserCommandHandler repo commands events))
