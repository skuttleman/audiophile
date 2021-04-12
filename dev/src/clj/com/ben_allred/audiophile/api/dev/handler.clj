(ns com.ben-allred.audiophile.api.dev.handler
  (:require
    [com.ben-allred.audiophile.api.services.repositories.users :as users]
    [com.ben-allred.audiophile.api.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.services.navigation :as nav]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]
    [ring.util.response :as res])
  (:import
    (java.net URI)))

(defn ^:private token->cookie [resp value cookie]
  (->> value
       (assoc {:path "/" :http-only true} :value)
       (merge cookie)
       (assoc-in resp [:cookies "auth-token"])))

(defn ^:private redirect
  ([route base-url value]
   (redirect route base-url value nil))
  ([route base-url value cookie]
   (let [path (cond
                (keyword? route) (nav/path-for route)
                (or (nil? route) (.isAbsolute (URI. route))) (nav/path-for :ui/home)
                :else route)]
     (-> base-url
         (str path)
         res/redirect
         (token->cookie value cookie)))))

(defn ^:private logout! [base-url]
  (redirect :ui/home base-url "" {:max-age 0}))

(defn ^:private login* [user jwt-serde base-url redirect-uri]
  (cond
    (seq user)
    (redirect redirect-uri base-url (serdes/serialize jwt-serde {:user user}))

    :else
    (logout! base-url)))

(defmethod ig/init-key ::login [_ {:keys [base-url]}]
  (fn [request]
    (log/info request)
    (let [params (get-in request [:nav/route :query-params])]
      (-> base-url
          (str (nav/path-for :auth/callback {:query-params (select-keys params #{:email :redirect-uri})}))
          res/redirect))))

(defmethod ig/init-key ::logout [_ {:keys [base-url]}]
  (fn [_]
    (logout! base-url)))

(defmethod ig/init-key ::callback [_ {:keys [base-url jwt-serde tx]}]
  (fn [request]
    (let [{:keys [email redirect-uri]} (get-in request [:nav/route :query-params])]
      (login* (users/query-by-email tx email)
              jwt-serde
              base-url
              redirect-uri))))

(defmethod ig/init-key ::app [_ {:keys [app]}]
  app)
