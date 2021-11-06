(ns com.ben-allred.audiophile.backend.api.handlers.auth
  (:require
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.backend.api.validations.selectors :as selectors]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]))

(defn login
  "Handles a request to authenticate in the system."
  [{:keys [auth]}]
  (fn [request]
    (pint/login auth request)))

(defmethod selectors/select [:get :auth/login]
  [_ request]
  (let [[state params] (maps/extract-keys (get-in request [:nav/route :params])
                                          #{:redirect-uri})]
    (cond-> params
      (seq state) (assoc :state state))))

(defn logout
  "Handles a request to revoke authentication in the system."
  [{:keys [auth]}]
  (fn [request]
    (pint/logout auth request)))

(defmethod selectors/select [:get :auth/logout]
  [_ request]
  (get-in request [:nav/route :params]))

(defn callback-url
  "Generates url for calling the system back to finish asynchronous authentication flow."
  [{:keys [base-url redirect-path]}]
  (str base-url redirect-path))

(defn callback
  "Handles a callback from external authentication service."
  [{:keys [auth]}]
  (fn [request]
    (pint/callback auth request)))

(defmethod selectors/select [:get :auth/callback]
  [_ request]
  (get-in request [:nav/route :params]))
