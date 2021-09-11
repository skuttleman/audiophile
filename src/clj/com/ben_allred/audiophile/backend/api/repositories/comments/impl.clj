(ns com.ben-allred.audiophile.backend.api.repositories.comments.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.comments.core :as rcomments]
    [com.ben-allred.audiophile.backend.api.repositories.common :as crepos]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]))

(defn ^:private create* [executor data opts]
  (crepos/with-access (rcomments/insert-comment-access? executor data opts)
    (let [comment-id (rcomments/insert-comment! executor data opts)
          comment (rcomments/find-event-comment executor comment-id)]
      (rcomments/comment-created! executor (:user/id opts) comment opts))))

(deftype CommentAccessor [repo]
  pint/ICommentAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo rcomments/select-for-file (:file/id opts) opts))
  (create! [_ data opts]
    (let [opts (assoc opts
                      :error/command :comment/create
                      :error/reason "insufficient access to create comment")]
      (crepos/command! repo opts
        (repos/transact! repo create* data opts)))))

(defn accessor
  "Constructor for [[CommentAccessor]] which provides semantic access for storing and retrieving comments."
  [{:keys [repo]}]
  (->CommentAccessor repo))
