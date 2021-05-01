(ns com.ben-allred.audiophile.common.utils.duct
  (:require
    #?@(:clj [[clojure.edn :as edn*]
              [duct.core.env :as env*]
              [duct.core :as duct]])
    [com.ben-allred.audiophile.common.utils.uuids :as uuids])
  #?(:clj
     (:import
       (java.io PushbackReader))))

#?(:clj
   (def readers
     "custom readers for parsing duct config files"
     {'audiophile/uuid-param (partial conj [uuids/regex])
      'audiophile/merge      (partial reduce duct/merge-configs {})}))

#?(:clj
   (defmethod env*/coerce 'Edn [s _]
     (edn*/read-string s)))
