(ns audiophile.backend.infrastructure.repositories.comments.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.domain.interactors.protocols :as pint]
    [audiophile.backend.infrastructure.repositories.comments.queries :as q]
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.logger :as log]))

(defmethod wf/->ctx :comments/create
  [_]
  '{:comment/body            ?body
    :comment/selection       ?selection
    :comment/file-version-id ?version-id
    :comment/comment-id      ?parent-id
    :user/id                 ?user-id})
(defmethod wf/->result :comments/create
  [_]
  '{:workflows/->result {:comment/id (sp.ctx/get ?comment-id)}})

(deftype CommentAccessor [repo ch]
  pint/ICommentAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo q/select-for-file (:file/id opts) opts))
  (create! [_ data opts]
    (ps/start-workflow! ch :comments/create (merge opts data) opts)))

(defn accessor
  "Constructor for [[CommentAccessor]] which provides semantic access for storing and retrieving comments."
  [{:keys [ch repo]}]
  (->CommentAccessor repo ch))
