(ns com.ben-allred.audiophile.backend.api.repositories.comments.impl
  (:refer-clojure :exclude [accessor])
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

(deftype CommentAccessor [repo commands events]
  pint/ICommentAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo rcomments/select-for-file (:file/id opts) opts))
  (create! [_ data opts]
    (ps/emit-command! commands (:user/id opts) :comment/create! data opts))

  pint/IMessageHandler
  (handle! [_ {[command-id {:command/keys [type] :as command} ctx] :msg :as msg}]
    (when (= type :comment/create!)
      (try
        (log/info "saving comment to db" command-id)
        (let [comment (repos/transact! repo create* (:command/data command) ctx)]
          (ps/emit-event! events (:user/id ctx) (:comment/id comment) :comment/created comment ctx))
        (catch Throwable ex
          (log/error ex "failed: saving comment to db" msg)
          (try
            (ps/command-failed! events
                                (:request/id ctx)
                                (assoc ctx
                                       :error/command (:command/type command)
                                       :error/reason (.getMessage ex)))
            (catch Throwable ex
              (log/error ex "failed to emit command/failed"))))))))

(defn accessor
  "Constructor for [[CommentAccessor]] which provides semantic access for storing and retrieving comments."
  [{:keys [commands events repo]}]
  (->CommentAccessor repo commands events))
