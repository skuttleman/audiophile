(ns audiophile.backend.infrastructure.repositories.comments.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.backend.infrastructure.repositories.comments.queries :as q]
    [audiophile.backend.domain.interactors.protocols :as pint]
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.common.core.utils.logger :as log]))

(deftype CommentAccessor [repo ch]
  pint/ICommentAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo q/select-for-file (:file/id opts) opts))
  (create! [_ data opts]
    (ps/emit-command! ch :comment/create! data opts)))

(defn accessor
  "Constructor for [[CommentAccessor]] which provides semantic access for storing and retrieving comments."
  [{:keys [ch repo]}]
  (->CommentAccessor repo ch))