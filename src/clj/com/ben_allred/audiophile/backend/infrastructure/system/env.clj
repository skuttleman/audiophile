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

(defn base-url
  "formats a base-url string. Uses existing base-url if one is supplied.

  ```clojure
  (base-url {:server-port 1234 :protocol \"https\"}) ;; => https://my-host:1234
  (base-url {:base-url \"protocol://some-base-url\"}) ;; => protocol://some-base-url
  ```"
  [{:keys [base-url host protocol server-port]}]
  (or base-url
      (format "%s://%s%s"
              (or protocol "http")
              (or host (.getCanonicalHostName (InetAddress/getLocalHost)))
              (cond-> "" server-port (str ":" server-port)))))
