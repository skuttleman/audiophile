(ns com.ben-allred.audiophile.backend.api.repositories.comments.core
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.comments.protocols :as pc]))

(defn select-for-file
  ([accessor file-id]
   (select-for-file accessor file-id nil))
  ([accessor file-id opts]
   (pc/select-for-file accessor file-id opts)))

(defn insert-comment-access?
  ([accessor comment]
   (insert-comment-access? accessor comment nil))
  ([accessor comment opts]
   (pc/insert-comment-access? accessor comment opts)))

(defn insert-comment!
  ([accessor comment]
   (insert-comment! accessor comment nil))
  ([accessor comment opts]
   (pc/insert-comment! accessor comment opts)))

(defn find-event-comment [accessor comment-id]
  (pc/find-event-comment accessor comment-id))
