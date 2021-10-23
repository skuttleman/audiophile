(ns com.ben-allred.audiophile.backend.infrastructure.pubsub.handlers.comments
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.comments.core :as rcomments]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.api.pubsub.core :as ps]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]))

(defn ^:private create* [executor comment opts]
  (if (rcomments/insert-comment-access? executor comment opts)
    (let [comment-id (rcomments/insert-comment! executor comment opts)]
      (rcomments/find-event-comment executor comment-id))
    (throw (ex-info "insufficient access" comment))))

(deftype CommentCommandHandler [repo ch]
  pint/IMessageHandler
  (handle? [_ msg]
    (= :comment/create! (:command/type msg)))
  (handle! [this {command-id :command/id :command/keys [ctx data type]}]
    (log/with-ctx [this :CP]
      (try
        (log/info "saving comment to db" command-id)
        (let [comment (repos/transact! repo create* data ctx)]
          (ps/emit-event! ch (:comment/id comment) :comment/created comment ctx))
        (catch Throwable ex
          (ps/command-failed! ch
                              (or (:request/id ctx)
                                  (uuids/random))
                              (assoc ctx
                                     :error/command type
                                     :error/reason (.getMessage ex)))
          (throw ex))))))

(defn msg-handler [{:keys [ch repo]}]
  (->CommentCommandHandler repo ch))
