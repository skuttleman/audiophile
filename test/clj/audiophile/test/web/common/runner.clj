(ns audiophile.test.web.common.runner
  (:require
    [clojure.string :as string]
    [audiophile.backend.infrastructure.system.env :as env]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.common.infrastructure.duct :as uduct]
    [audiophile.test.web.common.page :as pg]
    [duct.core :as duct]
    [duct.core.env :as env*]
    [integrant.core :as ig]
    audiophile.backend.dev.handler
    audiophile.backend.infrastructure.system.core)
  (:import
    (java.net ServerSocket)))

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

(defn wrap-run [run]
  (fn [{:kaocha.testable/keys [id] :as testable} plan]
    (pg/with-web id run-system!
      (run testable plan))))
