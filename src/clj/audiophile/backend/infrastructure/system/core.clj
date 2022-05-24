(ns audiophile.backend.infrastructure.system.core
  (:require
    [audiophile.backend.infrastructure.http.core :as handlers]
    [audiophile.backend.infrastructure.http.resources :as resources]
    [audiophile.backend.infrastructure.http.middleware :as middleware]
    [audiophile.backend.infrastructure.web :as web]
    [integrant.core :as ig]
    audiophile.backend.infrastructure.system.env
    audiophile.backend.infrastructure.system.handlers
    audiophile.backend.infrastructure.system.services.core
    audiophile.common.infrastructure.system.core))

(defmethod ig/init-key :audiophile.core/server [_ cfg]
  (web/server cfg))

(defmethod ig/halt-key! :audiophile.core/server [_ server]
  (web/server#stop server))

(defmethod ig/init-key :audiophile.spec/with-spec [_ cfg]
  (handlers/with-spec cfg))

(defmethod ig/init-key :audiophile.spec/ok [_ cfg]
  (handlers/ok cfg))

(defmethod ig/init-key :audiophile.spec/id [_ cfg]
  (handlers/id cfg))

(defmethod ig/init-key :audiophile.spec/no-content [_ cfg]
  (handlers/no-content cfg))

(defmethod ig/init-key :audiophile.middleware/vector-response [_ cfg]
  (middleware/vector-response cfg))

(defmethod ig/init-key :audiophile.middleware/with-serde [_ cfg]
  (middleware/with-serde cfg))

(defmethod ig/init-key :audiophile.middleware/with-route [_ cfg]
  (middleware/with-route cfg))

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
