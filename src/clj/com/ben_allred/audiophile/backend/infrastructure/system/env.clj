(ns com.ben-allred.audiophile.backend.infrastructure.system.env
  (:require
    [clojure.java.io :as io]
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.core.utils.core :as u]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    com.ben-allred.audiophile.ui.infrastructure.system)
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

(defn base-url [{:keys [base-url server-port]}]
  (or base-url
      (format "http://%s:%d" (.getCanonicalHostName (InetAddress/getLocalHost)) server-port)))
