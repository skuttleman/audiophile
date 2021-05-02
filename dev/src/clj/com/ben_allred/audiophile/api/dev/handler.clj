(ns com.ben-allred.audiophile.api.dev.handler
  (:require
    [com.ben-allred.audiophile.api.handlers.auth :as auth]
    [com.ben-allred.audiophile.api.utils.ring :as ring]
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(defmethod ig/init-key ::login [_ {:keys [base-url nav]}]
  (fn [request]
    (let [params (get-in request [:nav/route :query-params])]
      (-> base-url
          (str (nav/path-for nav
                             :auth/callback
                             {:query-params (select-keys params #{:email :redirect-uri})}))
          ring/redirect))))

(defmethod ig/init-key ::callback [_ {:keys [base-url jwt-serde nav user-repo]}]
  (fn [request]
    (let [{:keys [email]} (get-in request [:nav/route :query-params])]
      (auth/login! nav jwt-serde base-url user-repo email))))

(defn logging [app request]
  (if (or (string/starts-with? (:uri request "") "/api")
          (string/starts-with? (:uri request "") "/auth"))
    (log/spy :debug (app (log/spy :debug request)))
    (app request)))

(defmethod ig/init-key ::app [_ {:keys [app]}]
  (fn [request]
    (try (logging app request)
         (catch Throwable ex
           (log/error ex "[DEV] uncaught exception!")
           {:status 500}))))
