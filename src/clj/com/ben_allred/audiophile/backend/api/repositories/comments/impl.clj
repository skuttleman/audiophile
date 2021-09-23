(ns com.ben-allred.audiophile.backend.api.repositories.comments.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.comments.core :as rcomments]
    [com.ben-allred.audiophile.backend.api.repositories.common :as crepos]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.core :as ps]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn ^:private create* [executor comment opts]
  (if (rcomments/insert-comment-access? executor comment opts)
    (let [comment-id (rcomments/insert-comment! executor comment opts)]
      (rcomments/find-event-comment executor comment-id))
    (throw (ex-info "insufficient access" {}))))

(deftype CommentAccessor [repo]
  pint/ICommentAccessor
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo rcomments/select-for-file (:file/id opts) opts))
  (create! [_ data opts]
    (repos/transact! repo create* data opts)))

(defn accessor
  "Constructor for [[CommentAccessor]] which provides semantic access for storing and retrieving comments."
  [{:keys [repo]}]
  (->CommentAccessor repo))

(defn command-handler [{:keys [accessor pubsub]}]
  (letfn [(predicate [{[_ {:command/keys [type]}] :msg}]
            (= :comment/create! type))
          (handler [{[_ command ctx] :msg}]
            (let [comment (int/create! accessor (:command/data command) ctx)]
              (ps/emit-event! pubsub (:user/id ctx) (:comment/id comment) :comment/created comment ctx)))]
    (crepos/command-handler pubsub predicate "saving comment to db" handler)))
