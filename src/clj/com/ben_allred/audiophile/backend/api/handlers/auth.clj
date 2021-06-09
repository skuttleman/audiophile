(ns com.ben-allred.audiophile.backend.api.handlers.auth
  (:require
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn login [{:keys [auth]}]
  (fn [request]
    (pint/login auth request)))

(defn logout [{:keys [auth]}]
  (fn [request]
    (pint/logout auth request)))

(defn callback-url [{:keys [base-url auth-callback]}]
  (str base-url auth-callback))

(defn callback [{:keys [auth]}]
  (fn [request]
    (pint/callback auth request)))
