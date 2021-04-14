(ns com.ben-allred.audiophile.api.dev.handler
  (:require
    [com.ben-allred.audiophile.api.handlers.auth :as auth]
    [com.ben-allred.audiophile.api.services.repositories.users :as users]
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.services.navigation :as nav]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]
    [ring.util.response :as res])
  (:import
    (java.net URI)))

(defn ^:private redirect
  ([nav route base-url value]
   (redirect nav route base-url value nil))
  ([nav route base-url value cookie]
   (let [path (cond
                (keyword? route) (nav/path-for nav route)
                (or (nil? route) (.isAbsolute (URI. route))) (nav/path-for nav :ui/home)
                :else route)]
     (-> base-url
         (str path)
         res/redirect
         (auth/token->cookie value cookie)))))

(defn ^:private logout! [nav base-url]
  (redirect nav :ui/home base-url "" {:max-age 0}))

(defn ^:private login* [nav user jwt-serde base-url redirect-uri]
  (cond
    (seq user)
    (redirect nav redirect-uri base-url (serdes/serialize jwt-serde {:user user}))

    :else
    (logout! nav base-url)))

(defmethod ig/init-key ::login [_ {:keys [base-url nav]}]
  (fn [request]
    (let [params (get-in request [:nav/route :query-params])]
      (-> base-url
          (str (nav/path-for nav
                             :auth/callback
                             {:query-params (select-keys params #{:email :redirect-uri})}))
          res/redirect))))

(defmethod ig/init-key ::logout [_ {:keys [base-url nav]}]
  (fn [_]
    (logout! nav base-url)))

(defmethod ig/init-key ::callback [_ {:keys [base-url jwt-serde nav tx]}]
  (fn [request]
    (let [{:keys [email redirect-uri]} (get-in request [:nav/route :query-params])]
      (login* nav
              (users/query-by-email tx email)
              jwt-serde
              base-url
              redirect-uri))))

(defmethod ig/init-key ::app [_ {:keys [app]}]
  app)
