(ns com.ben-allred.audiophile.api.dev.handler
  (:require
    [clojure.java.io :as io]
    [clojure.java.shell :as sh]
    [clojure.string :as string]
    [com.ben-allred.audiophile.api.services.auth.protocols :as pauth]
    [com.ben-allred.audiophile.api.services.repositories.protocols :as prepos]
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(defmethod ig/init-key ::s3-client [_ _]
  (reify
    prepos/IKVStore
    (uri [_ key _]
      (str "local://target/" key))
    (get [_ key _]
      (let [file (io/file (str "target/" key ".dat"))]
        (if (.exists file)
          (io/input-stream file)
          (throw (ex-info "could not stub S3Client#get" {:key key})))))
    (put! [_ key value opts]
      (sh/sh "mkdir" "-p" "target/artifacts")
      (io/copy value (io/file (str "target/" key ".dat")))
      (.close value)
      (spit (str "target/" key ".edn")
            (pr-str {:ContentType   (:content-type opts)
                     :Metadata      (:metadata opts)
                     :ContentLength (:content-length opts)})))))

(defmethod ig/init-key ::oauth [_ {:keys [base-url nav]}]
  (reify
    pauth/IOAuthProvider
    (-redirect-uri [_ {:keys [email]}]
      (str base-url (nav/path-for nav :auth/callback {:query-params {:mock-email email}})))
    (-profile [_ opts]
      {:email (:mock-email opts)})))

(defn logging [app request]
  (if (or (string/starts-with? (:uri request "") "/api")
          (string/starts-with? (:uri request "") "/auth"))
    (log/spy :debug (app (log/spy :debug request)))
    (app request)))

(defmethod ig/init-key ::app [_ {:keys [app]}]
  (fn [request]
    (try (logging app request)
         (catch Throwable ex
           (log/error ex "[DEV] uncaught exception!" request)
           {:status 500}))))
