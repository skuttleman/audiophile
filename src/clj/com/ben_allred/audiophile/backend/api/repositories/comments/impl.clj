(ns com.ben-allred.audiophile.backend.api.repositories.comments.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.comments.core :as rcomments]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.api.pubsub.core :as ps]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(deftype CommentAccessor [repo ch]
  pint/ICommentAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo rcomments/select-for-file (:file/id opts) opts))
  (create! [_ data opts]
    (ps/emit-command! ch :comment/create! data opts)))

(defn accessor
  "Constructor for [[CommentAccessor]] which provides semantic access for storing and retrieving comments."
  [{:keys [ch repo]}]
  (->CommentAccessor repo ch))
