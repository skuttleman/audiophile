(ns com.ben-allred.audiophile.backend.api.repositories.comments.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.comments.core :as rcomments]
    [com.ben-allred.audiophile.backend.api.repositories.common :as crepos]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]))

(defn ^:private create* [executor data opts]
  (crepos/with-access (rcomments/insert-comment-access? executor data opts)
    (rcomments/insert-comment! executor data opts)))

(defn ^:private on-comment-created! [executor comment-id opts]
  (let [comment (rcomments/find-event-comment executor comment-id)]
    (rcomments/comment-created! executor (:user/id opts) comment opts)))

(deftype CommentAccessor [repo]
  pint/ICommentAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo rcomments/select-for-file (:file/id opts) opts))
  (create! [_ data opts]
    (let [opts (assoc opts
                      :error/command :comment/create
                      :error/reason "insufficient access to create comment"
                      :on-success on-comment-created!)]
      (crepos/command! repo opts create* data))))

(defn accessor
  "Constructor for [[CommentAccessor]] which provides semantic access for storing and retrieving comments."
  [{:keys [repo]}]
  (->CommentAccessor repo))
