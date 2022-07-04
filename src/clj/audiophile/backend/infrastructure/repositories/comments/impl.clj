(ns audiophile.backend.infrastructure.repositories.comments.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [audiophile.backend.domain.interactors.protocols :as pint]
    [audiophile.backend.infrastructure.repositories.comments.queries :as qcomments]
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.logger :as log]))

(deftype CommentAccessor [repo producer]
  pint/ICommentAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo qcomments/select-for-file (:file/id opts) opts))
  (create! [_ data opts]
    (wf/start-workflow! producer :comments/create (merge opts data) opts)))

(defn accessor
  "Constructor for [[CommentAccessor]] which provides semantic access for storing and retrieving comments."
  [{:keys [producer repo]}]
  (->CommentAccessor repo producer))
