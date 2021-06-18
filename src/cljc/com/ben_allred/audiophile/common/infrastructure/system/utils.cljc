(ns com.ben-allred.audiophile.common.infrastructure.system.utils
  (:require [integrant.core :as ig]))

(defmethod ig/init-key :audiophile.utils/ref-map [_ {:keys [id-fn refset]}]
  (into {} (map (juxt id-fn identity)) refset))
