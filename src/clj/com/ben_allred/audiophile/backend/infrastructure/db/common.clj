(ns com.ben-allred.audiophile.backend.infrastructure.db.common
  (:require
    [clojure.set :as set]
    [com.ben-allred.audiophile.backend.api.repositories.common :as crepos]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.events.core :as events]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn access? [executor query]
  (boolean (seq (repos/execute! executor query))))

(defn event->db-handler [{:keys [predicate repo]
                          :or   {predicate (constantly true)}}]
  (crepos/msg-handler predicate
                      "saving event to db"
                      (fn [{[_ event] :msg}]
                        (repos/transact! repo
                                         events/insert-event!
                                         event
                                         (set/rename-keys event {:event/emitted-by :user/id})))))
