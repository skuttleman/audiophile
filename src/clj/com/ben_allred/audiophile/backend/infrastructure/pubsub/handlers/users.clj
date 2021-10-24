(ns com.ben-allred.audiophile.backend.infrastructure.pubsub.handlers.users
  (:require
    [com.ben-allred.audiophile.backend.api.pubsub.core :as ps]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.users.core :as rusers]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [clojure.set :as set]))

(defn ^:private create* [executor user opts]
  {:user/id (rusers/insert-user! executor user opts)})

(deftype UserCommandHandler [repo commands events]
  pint/IMessageHandler
  (handle? [_ msg]
    (= :user/create! (:command/type msg)))
  (handle! [this {command-id :command/id :command/keys [ctx data type]}]
    (log/with-ctx [this :CP]
      (let [user (repos/transact! repo create* data ctx)
            ctx (-> ctx
                    (set/rename-keys {:user/id :signup/id})
                    (assoc :user/id (:user/id user)))]
        (try
          (log/info "saving user to db" command-id)
          (ps/emit-command! commands
                            :team/create!
                            {:team/name "My Personal Projects"
                             :team/type :PERSONAL}
                            ctx)
          (ps/emit-event! events (:user/id user) :user/created user ctx)
          (catch Throwable ex
            (ps/command-failed! events
                                (or (:request/id ctx)
                                    (uuids/random))
                                (assoc ctx
                                       :error/command type
                                       :error/reason (.getMessage ex)))
            (throw ex)))))))

(defn msg-handler [{:keys [commands events repo]}]
  (->UserCommandHandler repo commands events))
