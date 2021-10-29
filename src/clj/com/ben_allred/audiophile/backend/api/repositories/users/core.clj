(ns com.ben-allred.audiophile.backend.api.repositories.users.core
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.users.protocols :as pu]))

(defn find-by-id
  ([accessor user-id]
   (find-by-id accessor user-id nil))
  ([accessor user-id opts]
   (pu/find-by-id accessor user-id opts)))

(defn find-by-email
  ([accessor email]
   (find-by-email accessor email nil))
  ([accessor email opts]
   (pu/find-by-email accessor email opts)))

(defn insert-user!
  ([accessor user]
   (insert-user! accessor user nil))
  ([accessor user opts]
   (pu/insert-user! accessor user opts)))
