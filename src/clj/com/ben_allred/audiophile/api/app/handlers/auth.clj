(ns com.ben-allred.audiophile.api.app.handlers.auth
  (:require
    [com.ben-allred.audiophile.api.app.interactors.protocols :as pint]
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

(defn details [{:keys [auth]}]
  (fn [request]
    (pint/details auth request)))
