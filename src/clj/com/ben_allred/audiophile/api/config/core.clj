(ns com.ben-allred.audiophile.api.config.core
  (:require
    [com.ben-allred.audiophile.api.handlers.middleware :as middleware]
    [com.ben-allred.audiophile.api.handlers.resources :as resources]
    [com.ben-allred.audiophile.api.handlers.validations.core :as validations]
    [com.ben-allred.audiophile.api.core :as api]
    [com.ben-allred.audiophile.api.services.env :as env]
    [integrant.core :as ig]
    com.ben-allred.audiophile.api.config.handlers
    com.ben-allred.audiophile.api.config.services.core))

(defmethod ig/init-key :audiophile.core/server [_ cfg]
  (api/server cfg))

(defmethod ig/halt-key! :audiophile.core/server [_ server]
  (api/server#stop server))

(defmethod ig/init-key :audiophile.services.env/base-url [_ cfg]
  (env/base-url cfg))

(defmethod ig/init-key :audiophile.spec/with-spec [_ cfg]
  (validations/with-spec cfg))

(defmethod ig/init-key :audiophile.spec/ok [_ cfg]
  (validations/ok cfg))

(defmethod ig/init-key :audiophile.spec/id [_ cfg]
  (validations/id cfg))

(defmethod ig/init-key :audiophile.middleware/vector-response [_ cfg]
  (middleware/vector-response cfg))

(defmethod ig/init-key :audiophile.middleware/with-serde [_ cfg]
  (middleware/with-serde cfg))

(defmethod ig/init-key :audiophile.middleware/with-route [_ cfg]
  (middleware/with-route cfg))

(defmethod ig/init-key :audiophile.middleware/with-logging [_ cfg]
  (middleware/with-logging cfg))

(defmethod ig/init-key :audiophile.middleware/with-auth [_ cfg]
  (middleware/with-auth cfg))

(defmethod ig/init-key :audiophile.middleware/with-headers [_ cfg]
  (middleware/with-headers cfg))

(defmethod ig/init-key :audiophile.middleware/with-cors [_ cfg]
  (middleware/with-cors cfg))

(defmethod ig/init-key :audiophile.routes/assets [_ cfg]
  (resources/assets cfg))

(defmethod ig/init-key :audiophile.routes/health [_ cfg]
  (resources/health cfg))

(defmethod ig/init-key :audiophile.routes/ui [_ cfg]
  (resources/ui cfg))
