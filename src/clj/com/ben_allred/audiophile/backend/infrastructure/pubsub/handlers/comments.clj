(ns com.ben-allred.audiophile.backend.infrastructure.pubsub.handlers.comments
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.comments.core :as rcomments]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.core :as ps]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn ^:private create* [executor comment opts]
  (if (rcomments/insert-comment-access? executor comment opts)
    (let [comment-id (rcomments/insert-comment! executor comment opts)]
      (rcomments/find-event-comment executor comment-id))
    (throw (ex-info "insufficient access" comment))))

(deftype CommentCommandHandler [repo pubsub]
  pint/IMessageHandler
  (handle! [_ {[command-id {:command/keys [type] :as command} ctx] :msg :as msg}]
    (when (= type :comment/create!)
      (try
        (log/info "saving comment to db" command-id)
        (let [comment (repos/transact! repo create* (:command/data command) ctx)]
          (ps/emit-event! pubsub (:user/id ctx) (:comment/id comment) :comment/created comment ctx))
        (catch Throwable ex
          (log/error ex "failed: saving comment to db" msg)
          (try
            (ps/command-failed! pubsub
                                (:request/id ctx)
                                (assoc ctx
                                       :error/command (:command/type command)
                                       :error/reason (.getMessage ex)))
            (catch Throwable ex
              (log/error ex "failed to emit command/failed"))))))))

(defn msg-handler [{:keys [repo pubsub]}]
  (->CommentCommandHandler repo pubsub))
