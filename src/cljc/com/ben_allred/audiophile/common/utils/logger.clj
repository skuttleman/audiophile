(ns com.ben-allred.audiophile.common.utils.logger
  (:require
    [taoensso.timbre :as log*]))

(def ^:macro ^{:arglists '([& args])} debug (var-get #'log*/debug))
(def ^:macro ^{:arglists '([& args])} info (var-get #'log*/info))
(def ^:macro ^{:arglists '([& args])} warn (var-get #'log*/warn))
(def ^:macro ^{:arglists '([& args])} error (var-get #'log*/error))
(def ^:macro ^{:arglists '([form] [level form])} spy (var-get #'log*/spy))

(defn ^:private clean [data]
  (assoc data :hostname_ (delay nil)))

(log*/merge-config! {:level      (keyword (or (System/getenv "LOG_LEVEL")
                                              :info))
                     :middleware [clean]})
