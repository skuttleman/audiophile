(ns com.ben-allred.audiophile.api.services.env
  (:require
    [clojure.java.io :as io]
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.utils.core :as u]
    [integrant.core :as ig])
  (:import
    (java.net InetAddress)))

(defn ^:private file->env [file]
  (u/silent!
    (some->> file
             io/file
             slurp
             (serdes/deserialize (ig/init-key ::serdes/edn nil)))))

(defn load-env [files]
  (transduce (map file->env)
             merge
             {}
             files))

(defmethod ig/init-key ::base-url [_ {:keys [base-url server-port]}]
  (or base-url
      (format "http://%s:%d" (.getCanonicalHostName (InetAddress/getLocalHost)) server-port)))
