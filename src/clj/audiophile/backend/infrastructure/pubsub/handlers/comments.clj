(ns audiophile.backend.infrastructure.pubsub.handlers.comments
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.api.repositories.comments.core :as rcomments]
    [audiophile.backend.api.repositories.core :as repos]
    [audiophile.backend.domain.interactors.protocols :as pint]
    [audiophile.backend.infrastructure.pubsub.handlers.common :as hc]
    [audiophile.common.core.utils.logger :as log]))

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
      (hc/with-command-failed! [ch type ctx]
        (log/info "saving comment to db" command-id)
        (let [comment (repos/transact! repo create* data ctx)]
          (ps/emit-event! ch (:comment/id comment) :comment/created comment ctx))))))

(defn msg-handler [{:keys [ch repo]}]
  (->CommentCommandHandler repo ch))
