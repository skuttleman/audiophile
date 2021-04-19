(ns com.ben-allred.audiophile.api.dev.handler
  (:require
    [com.ben-allred.audiophile.api.handlers.auth :as auth]
    [com.ben-allred.audiophile.api.services.repositories.users :as users]
    [com.ben-allred.audiophile.api.utils.ring :as ring]
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [integrant.core :as ig]))

(defmethod ig/init-key ::login [_ {:keys [base-url nav]}]
  "GET /auth/login - dev only implementation"
  (fn [request]
    (let [params (get-in request [:nav/route :query-params])]
      (-> base-url
          (str (nav/path-for nav
                             :auth/callback
                             {:query-params (select-keys params #{:email :redirect-uri})}))
          ring/redirect))))

(defmethod ig/init-key ::callback [_ {:keys [base-url jwt-serde nav tx]}]
  "GET /auth/callback - dev only implementation"
  (fn [request]
    (let [{:keys [email redirect-uri]} (get-in request [:nav/route :query-params])]
      (auth/login! nav
                   (users/query-by-email tx email)
                   jwt-serde
                   base-url
                   redirect-uri))))

(defmethod ig/init-key ::app [_ {:keys [app]}]
  "for when dev modifications are needed (like additional middleware)"
  app)
