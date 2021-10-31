(ns com.ben-allred.audiophile.test.web
  (:require
    [com.ben-allred.audiophile.backend.infrastructure.system.env :as env]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.infrastructure.duct :as uduct]
    [com.ben-allred.audiophile.test.utils.selenium :as selenium]
    [duct.core :as duct]
    [duct.core.env :as env*]
    [integrant.core :as ig]
    com.ben-allred.audiophile.backend.dev.accessors
    com.ben-allred.audiophile.backend.dev.handler
    com.ben-allred.audiophile.backend.infrastructure.system.core
    com.ben-allred.audiophile.test.integration.common.components
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [clojure.string :as string]
    [com.ben-allred.audiophile.common.core.utils.core :as u])
  (:import
    (java.net ServerSocket)
    (org.apache.commons.io.output NullWriter)))

(def ^:dynamic *system*)

(defn ^:private with-test-cfg [cfg]
  (let [port (with-open [server (ServerSocket. 0)]
               (.getLocalPort server))
        base-url (str "http://localhost:" port)
        ns (string/replace (str "test." (uuids/random)) #"-" "")]
    (assoc cfg
           "MQ_NAMESPACE" ns
           "MQ_CONSUMER_GROUP" "web"
           "PORT" (str port)
           "API_BASE_URL" base-url
           "AUTH_BASE_URL" base-url
           "UI_BASE_URL" base-url)))

(defn run-system! []
  (duct/load-hierarchy)
  (binding [env*/*env* (-> env*/*env*
                           (merge (env/load-env [".env-common" ".env-dev" ".env-test" ".env-test-web"]))
                           with-test-cfg)]
    (-> "web.edn"
        duct/resource
        (duct/read-config uduct/readers)
        (assoc-in [:duct.profile/base [:duct.custom/merge :routes/table]]
                  #{(ig/ref :routes/table#api)
                    (ig/ref :routes/table#auth)
                    (ig/ref :routes/table#jobs)
                    (ig/ref :routes/table#ui)})
        (duct/prep-config [:duct.profile/base :duct.profile/dev :duct.profile/test])
        (ig/init [:duct/daemon
                  :routes/daemon#api
                  :routes/daemon#auth
                  :routes/daemon#jobs
                  :routes/daemon#ui]))))

(defmacro with-driver [[sym] & body]
  `(let [~sym (binding [*out* NullWriter/NULL_WRITER]
                (selenium/create-driver nil))]
     (try ~@body
          (finally
            (binding [*out* NullWriter/NULL_WRITER]
              (u/silent!
                (selenium/close! ~sym)))))))

(defmacro with-web [& body]
  `(binding [*system* (binding [*out* NullWriter/NULL_WRITER]
                        (run-system!))]
     (try ~@body
          (finally
            (binding [*out* NullWriter/NULL_WRITER]
              (u/silent!
                (ig/halt! *system*)))))))

(defn wrap-run [run]
  (fn [testable plan]
    (if-not (= :web (:kaocha.testable/id testable))
      (run testable plan)
      (with-web (run testable plan)))))

(defn visit! [driver path]
  (selenium/visit! driver (str (get *system* [:duct/const :env/base-url#ui]) path)))

(defn wait-by-css! [driver selector]
  (selenium/wait-for! driver (fn [ctx]
                               (first (selenium/find-by ctx (selenium/by-css selector))))))

(defn fill-out-form!
  ([driver m]
   (fill-out-form! driver "body" m))
  ([driver selector m]
   (let [form (wait-by-css! driver selector)]
     (doseq [[selector value] m]
       (-> form
           (selenium/find-by (selenium/by-css selector))
           colls/only!
           (selenium/input! value))))))
