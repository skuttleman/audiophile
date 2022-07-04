(ns audiophile.backend.domain.interactors.core
  (:require
    [audiophile.backend.domain.interactors.protocols :as pint]))

(def ^:const INVALID_INPUT :interactor/INVALID_INPUT)
(def ^:const NOT_AUTHENTICATED :interactor/NOT_AUTHENTICATED)
(def ^:const INTERNAL_ERROR :interactor/INTERNAL_ERROR)

(defn ^:private throw-reason! [code]
  (throw (ex-info (name code) {:interactor/reason code})))

(defn invalid-input! []
  (throw-reason! INVALID_INPUT))

(defn not-authenticated! []
  (throw-reason! NOT_AUTHENTICATED))

(defn internal-error! []
  (throw-reason! INTERNAL_ERROR))

(defn query-many [accessor opts]
  (pint/query-many accessor opts))

(defn query-one [accessor opts]
  (pint/query-one accessor opts))

(defn create! [accessor data opts]
  (pint/create! accessor data opts))

(defn create-artifact! [interactor data opts]
  (pint/create-artifact! interactor data opts))

(defn create-file! [interactor data opts]
  (pint/create-file! interactor data opts))

(defn create-file-version! [interactor data opts]
  (pint/create-file-version! interactor data opts))

(defn get-artifact [interactor opts]
  (pint/get-artifact interactor opts))
