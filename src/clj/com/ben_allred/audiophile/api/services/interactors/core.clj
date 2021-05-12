(ns com.ben-allred.audiophile.api.services.interactors.core
  (:require
    [com.ben-allred.audiophile.api.services.interactors.protocols :as pint]))

(def ^:const MISSING_USER_CONTEXT :interactor/MISSING_USER_CTX)
(def ^:const INVALID_INPUT :interactor/INVALID_INPUT)

(defn ^:private throw-reason! [code]
  (throw (ex-info (name code) {:interactor/reason code})))

(defn missing-user-ctx! []
  (throw-reason! MISSING_USER_CONTEXT))

(defn invalid-input! []
  (throw-reason! INVALID_INPUT))

(defn query-many [accessor opts]
  (pint/query-many accessor opts))

(defn query-one [accessor opts]
  (pint/query-one accessor opts))

(defn create! [accessor data opts]
  (pint/create! accessor data opts))

(defn create-artifact! [interactor artifact opts]
  (pint/create-artifact! interactor artifact opts))

(defn create-file! [interactor project-id file opts]
  (pint/create-file! interactor project-id file opts))

(defn create-file-version! [interactor file-id version opts]
  (pint/create-file-version! interactor file-id version opts))
