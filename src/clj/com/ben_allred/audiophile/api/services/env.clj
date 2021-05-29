(ns com.ben-allred.audiophile.api.services.env
  (:require
    [clojure.java.io :as io]
    [com.ben-allred.audiophile.common.services.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.utils.core :as u]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]
    com.ben-allred.audiophile.ui.config)
  (:import
    (java.net InetAddress)))

(defn ^:private file->env [file]
  (u/silent!
    (some->> file
             io/file
             slurp
             (serdes/deserialize (serdes/edn {})))))

(defn load-env
  "Loads edn files and builds a map of environment variables. Silently skips files that don't exist."
  [files]
  (transduce (map file->env)
             merge
             {}
             files))

(defmethod ig/init-key ::base-url [_ {:keys [base-url server-port]}]
  (or base-url
      (format "http://%s:%d" (.getCanonicalHostName (InetAddress/getLocalHost)) server-port)))
