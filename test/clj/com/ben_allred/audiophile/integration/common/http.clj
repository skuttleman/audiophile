(ns com.ben-allred.audiophile.integration.common.http
  (:refer-clojure :exclude [get])
  (:require
    [clojure.string :as string]
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.utils.maps :as maps]))

(defn login
  ([request system]
   (login request system nil))
  ([request system user]
   (let [serde (clojure.core/get system [:duct/const :serdes/jwt])
         token (serdes/serialize serde user)]
     (assoc-in request [:headers "cookie"] (str "auth-token=" token)))))

(defn ^:private go [request system method page params]
  (let [nav (clojure.core/get system [:duct/const :services/nav])
        [uri query-string] (string/split (nav/path-for nav page params) #"\?")]
    (maps/assoc-maybe request
                      :request-method method
                      :uri uri
                      :query-string query-string)))

(defn get
  ([request system page]
   (get request system page nil))
  ([request system page params]
   (go request system :get page params)))

(defn with-serde
  ([handler system serde]
   (with-serde handler (clojure.core/get system [:duct/const serde])))
  ([handler serde]
   (let [mime-type (serdes/mime-type serde)]
     (fn [request]
       (-> request
           (update :headers assoc "content-type" mime-type "accept" mime-type)
           (maps/update-maybe :body (partial serdes/serialize serde))
           handler
           (maps/update-maybe :body (partial serdes/deserialize serde)))))))
