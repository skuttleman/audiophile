(ns audiophile.backend.infrastructure.pubsub.handlers.users
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.api.repositories.core :as repos]
    [audiophile.backend.api.repositories.search.core :as search]
    [audiophile.backend.api.repositories.users.core :as rusers]
    [audiophile.backend.domain.interactors.protocols :as pint]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.core.utils.uuids :as uuids]
    [clojure.set :as set]))

(defn ^:private query-signup-conflicts [executor user opts]
  (loop [[[field f] :as fields] [[:user/email rusers/find-by-email]
                                 [:user/handle search/find-by-handle]
                                 [:user/mobile-number search/find-by-mobile-number]]
         conflicts {}]
    (let [val (get user field)]
      (if (empty? fields)
        conflicts
        (recur (rest fields) (cond-> conflicts
                               (and val (some? (f executor val opts)))
                               (assoc [field] val)))))))

(defn ^:private create* [executor user opts]
  (when-let [fields (not-empty (query-signup-conflicts executor user opts))]
    (throw (ex-info "one or more unique fields are present" {:conflicts fields})))
  {:user/id (rusers/insert-user! executor user opts)})

(defmacro ^:private with-command-failed! [events command & body]
  `(try
     ~@body
     (catch Throwable ex#
       (let [{ctx# :command/ctx type# :command/type} ~command]
         (ps/command-failed! ~events
                             (or (:request/id ctx#)
                                 (uuids/random))
                             (maps/assoc-maybe ctx#
                                               :error/command type#
                                               :error/reason (.getMessage ex#)
                                               :error/details (not-empty (ex-data ex#))))
         (throw ex#)))))

(deftype UserCommandHandler [repo commands events]
  pint/IMessageHandler
  (handle? [_ msg]
    (= :user/create! (:command/type msg)))
  (handle! [this {command-id :command/id :command/keys [ctx data] :as command}]
    (log/with-ctx [this :CP]
      (log/info "saving user to db" command-id)
      (let [user (with-command-failed! events command
                   (repos/transact! repo create* data ctx))
            ctx (-> ctx
                    (set/rename-keys {:user/id :signup/id})
                    (assoc :user/id (:user/id user)))]
        (with-command-failed! events (assoc command :command/ctx ctx)
          (ps/emit-command! commands
                            :team/create!
                            {:team/name "My Personal Projects"
                             :team/type :PERSONAL}
                            ctx)
          (ps/emit-event! events (:user/id user) :user/created user ctx))))))

(defn msg-handler [{:keys [commands events repo]}]
  (->UserCommandHandler repo commands events))
