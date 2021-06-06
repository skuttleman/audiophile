(ns com.ben-allred.audiophile.api.domain.interactors.core
  (:require
    [com.ben-allred.audiophile.api.domain.interactors.protocols :as pint]))

(def ^:const INVALID_INPUT :interactor/INVALID_INPUT)
(def ^:const NOT_AUTHENTICATED :interactor/NOT_AUTHENTICATED)
(def ^:const NOT_IMPLEMENTED :interactor/NOT_IMPLEMENTED)

(defn ^:private throw-reason! [code]
  (throw (ex-info (name code) {:interactor/reason code})))

(defn invalid-input! []
  (throw-reason! INVALID_INPUT))

(defn not-authenticated! []
  (throw-reason! NOT_AUTHENTICATED))

(defn not-implemented! []
  (throw-reason! NOT_IMPLEMENTED))

(defn query-many [accessor opts]
  (pint/query-many accessor opts))

(defn query-one [accessor opts]
  (pint/query-one accessor opts))

(defn create! [accessor opts]
  (pint/create! accessor opts))

(defn create-artifact! [interactor opts]
  (pint/create-artifact! interactor opts))

(defn create-file! [interactor opts]
  (pint/create-file! interactor opts))

(defn create-file-version! [interactor opts]
  (pint/create-file-version! interactor opts))

(defn get-artifact [interactor opts]
  (pint/get-artifact interactor opts))