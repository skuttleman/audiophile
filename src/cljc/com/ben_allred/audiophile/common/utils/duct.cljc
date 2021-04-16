(ns com.ben-allred.audiophile.common.utils.duct
  (:require
    [clojure.edn :as edn*]
    [com.ben-allred.audiophile.common.utils.uuids :as uuids]
    [duct.core.env :as env*]))

(def readers
  {'audiophile/uuid-re (partial conj [uuids/regex])})

(defmethod env*/coerce 'Edn [s _]
  (edn*/read-string s))
