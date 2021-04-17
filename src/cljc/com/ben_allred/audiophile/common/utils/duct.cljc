(ns com.ben-allred.audiophile.common.utils.duct
  (:require
    #?@(:clj [[clojure.edn :as edn*]
              [duct.core.env :as env*]])
    [com.ben-allred.audiophile.common.utils.uuids :as uuids]
    [duct.core :as duct])
  #?(:clj
     (:import
       (java.io PushbackReader))))

(def readers
  {'audiophile/uuid-re (partial conj [uuids/regex])
   'audiophile/merge   (partial reduce duct/merge-configs {})})

#?(:clj
   (defmethod env*/coerce 'Edn [s _]
     (edn*/read-string s)))
