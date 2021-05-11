(ns com.ben-allred.audiophile.api.services.interactors.common)

(def ^:const MISSING_USER_CONTEXT :interactor/MISSING_USER_CTX)
(def ^:const INVALID_INPUT :interactor/INVALID_INPUT)

(defn ^:private throw-reason! [code]
  (throw (ex-info (name code) {:interactor/reason code})))

(defn missing-user-ctx! []
  (throw-reason! MISSING_USER_CONTEXT))

(defn invalid-input! []
  (throw-reason! INVALID_INPUT))
