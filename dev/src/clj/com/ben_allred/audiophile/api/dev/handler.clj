(ns com.ben-allred.audiophile.api.dev.handler
  (:require
    [clojure.edn :as edn*]
    [clojure.java.io :as io]
    [clojure.java.shell :as sh]
    [clojure.string :as string]
    [com.ben-allred.audiophile.api.handlers.auth :as auth]
    [com.ben-allred.audiophile.api.services.repositories.protocols :as prepos]
    [com.ben-allred.audiophile.api.utils.ring :as ring]
    [com.ben-allred.audiophile.common.services.navigation.core :as nav]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig])
  (:import
    (java.nio.file Files LinkOption)
    (java.nio.file.attribute BasicFileAttributes)
    (java.util Date)))

(defmethod ig/init-key ::s3-client [_ _]
  (reify
    prepos/IKVStore
    (uri [_ key _]
      (str "local://target/" key))
    (get [_ key _]
      (Thread/sleep 1000)
      (let [file (io/file (str "target/" key ".dat"))
            attrs (when (.exists file)
                    (Files/readAttributes (.toPath file) BasicFileAttributes (make-array LinkOption 0)))]
        (if attrs
          (assoc (edn*/read-string (slurp (str "target/" key ".edn")))
                 :LastModified (Date/from (.toInstant (.creationTime attrs)))
                 :Body (io/input-stream file))
          (throw (ex-info "could not stub S3Client#get" {:key key})))))
    (put! [_ key value opts]
      (Thread/sleep 1000)
      (sh/sh "mkdir" "-p" "target/artifacts")
      (io/copy value (io/file (str "target/" key ".dat")))
      (spit (str "target/" key ".edn")
            (pr-str {:ContentType   (:content-type opts)
                     :Metadata      (update (:metadata opts) :size str)
                     :ContentLength (get-in opts [:metadata :size])})))))

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
