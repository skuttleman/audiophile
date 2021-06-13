(ns com.ben-allred.audiophile.backend.api.repositories.users.core
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.users.protocols :as pu]))

(defn find-by-email
  ([accessor email]
   (find-by-email accessor email nil))
  ([accessor email opts]
   (pu/find-by-email accessor email opts)))
