(ns com.ben-allred.audiophile.api.dev-server
  (:require
    [com.ben-allred.audiophile.api.server :as server]
    [com.ben-allred.audiophile.api.services.env :as env]
    [com.ben-allred.audiophile.api.templates.core :as templates]
    [com.ben-allred.audiophile.common.views.core :as views]
    [nrepl.server :as nrepl]
    [ring.middleware.reload :as reload]
    [ring.middleware.resource :as res]
    [clojure.string :as string])
  (:import
    (java.net InetAddress)))

(defn ^:private index [handler]
  (fn [request]
    (if (= "/" (:uri request))
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (templates/html [views/app {}])}
      (handler request))))

(defn ^:private resources [handler]
  (fn [request]
    (or (some-> request
                (res/resource-request "public")
                (assoc-in [:headers "Content-Type"]
                          (cond
                            (string/ends-with? (:uri request) ".js") "application/javascript"
                            (string/ends-with? (:uri request) ".css") "text/css"
                            :else "text/plain")))
        (handler request))))

(def ^:wrap-reload app
  (-> #'server/app
      index
      resources
      (reload/wrap-reload {:dirs ["src/clj" "src/cljc"]})))

(defn -main [& {:as env}]
  (env/load-env! env ".env-dev")
  (let [server-port (env/get :server-port)
        repl-port (env/get :nrepl-port)
        base-url (format "http://%s:%d"
                         (.getCanonicalHostName (InetAddress/getLocalHost))
                         server-port)]
    (nrepl/start-server :port repl-port)
    (println "[nREPL] is listening on port" repl-port)
    (alter-var-root #'env/get assoc :base-url base-url :dev? true)
    (server/run-server! #'app)))


