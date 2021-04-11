(ns com.ben-allred.audiophile.api.server
  (:require
    [com.ben-allred.audiophile.api.services.env :as env]
    [immutant.web :as web]))

(defn app [request]
  (println request)
  {:status 200 :body "{\"some\":\"json\"}"})

(defn run-server! [app]
  (let [server-port (env/get :server-port)]
    (web/run app {:port server-port :host "0.0.0.0"})
    (println (str "[SERVER] is listening "
                  (when (:wrap-reload (meta app))
                    "with dev reloading enabled ")
                  "on port "
                  server-port))))

(defn -main [& {:as env}]
  (env/load-env! env ".env-prod")
  (run-server! #'app))
