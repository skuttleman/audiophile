(ns com.ben-allred.audiophile.backend.dev.handler
  (:require
    [clojure.java.io :as io]
    [clojure.java.shell :as sh]
    [com.ben-allred.audiophile.backend.api.protocols :as papp]
    [com.ben-allred.audiophile.backend.api.repositories.protocols :as prepos]
    [com.ben-allred.audiophile.common.api.navigation.core :as nav]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [integrant.core :as ig]))

(deftype DevS3Client []
  prepos/IKVStore
  (uri [_ key _]
    (str "local://target/" key))
  (get [_ key _]
    (let [file (io/file (str "target/" key ".dat"))]
      (when (.exists file)
        {:Body (io/input-stream file)})))
  (put! [_ key value opts]
    (sh/sh "mkdir" "-p" "target/artifacts")
    (io/copy value (io/file (str "target/" key ".dat")))
    (spit (str "target/" key ".edn")
          (pr-str {:ContentType   (:content-type opts)
                   :Metadata      (:metadata opts)
                   :ContentLength (:content-length opts)}))))

(defmethod ig/init-key :audiophile.dev/s3-client [_ _]
  (->DevS3Client))

(deftype DevOauthProvider [base-url nav]
  papp/IOAuthProvider
  (redirect-uri [_ {:keys [email]}]
    (str base-url (nav/path-for nav :auth/callback {:params {:mock-email email}})))
  (profile [_ opts]
    {:email (:mock-email opts)}))

(defmethod ig/init-key :audiophile.dev/oauth [_ {:keys [base-url nav]}]
  (->DevOauthProvider base-url nav))

(defn ->dev-app [app]
  "Wraps app handler to log uncaught exceptions and potentially any other dev concerns."
  (fn [request]
    (try (app request)
         (catch Throwable ex
           (log/error ex "[DEV] uncaught exception!" request)
           {:status 500}))))

(defmethod ig/init-key :audiophile.dev/app [_ {:keys [app]}]
  (->dev-app app))
