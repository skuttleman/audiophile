(ns com.ben-allred.audiophile.api.services.env
  (:require
    [integrant.core :as ig])
  (:import
    (java.net InetAddress)))

(defmethod ig/init-key ::base-url [_ {:keys [base-url server-port]}]
  (or base-url
      (format "http://%s:%d" (.getCanonicalHostName (InetAddress/getLocalHost)) server-port)))
