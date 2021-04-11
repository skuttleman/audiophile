(ns com.ben-allred.audiophile.common.utils.logger
  (:require
    [taoensso.timbre :as log*]))

(def ^:macro debug (var-get #'log*/debug))
(def ^:macro info (var-get #'log*/info))
(def ^:macro warn (var-get #'log*/warn))
(def ^:macro error (var-get #'log*/error))
(def ^:macro spy (var-get #'log*/spy))

(defn clean [data]
  (assoc data :hostname_ (delay nil)))

(log*/merge-config! {:level      (keyword (or (System/getenv "LOG_LEVEL")
                                              :info))
                     :middleware [clean]})
