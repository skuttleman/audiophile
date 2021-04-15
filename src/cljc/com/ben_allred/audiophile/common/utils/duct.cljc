(ns com.ben-allred.audiophile.common.utils.duct
  (:require
    [com.ben-allred.audiophile.common.utils.uuids :as uuids]))

(def readers
  {'audiophile/uuid-re (partial conj [uuids/regex])})
