(ns com.ben-allred.audiophile.common.utils.logger
  (:require
    [clojure.string :as string]
    [taoensso.timbre :as log*]))

(def ^:dynamic *ctx* nil)

(defmacro with-ctx [ctx & body]
  (if (:ns &env)
    `(do ~@body)
    `(binding [*ctx* (merge *ctx* ~ctx)]
       ~@body)))

(defmacro log [level line & args]
  `(when-not (:disabled? *ctx*) ;; TBD - ctx
     (log*/log! ~level :p ~args {:?line ~line})))

(defmacro trace [& args]
  `(log :trace ~(:line (meta &form)) ~@args))

(defmacro debug [& args]
  `(log :debug ~(:line (meta &form)) ~@args))

(defmacro info [& args]
  `(log :info ~(:line (meta &form)) ~@args))

(defmacro warn [& args]
  `(log :warn ~(:line (meta &form)) ~@args))

(defmacro error [& args]
  `(log :error ~(:line (meta &form)) ~@args))

(defmacro fatal [& args]
  `(log :fatal ~(:line (meta &form)) ~@args))

(defmacro report [& args]
  `(log :report ~(:line (meta &form)) ~@args))

(defmacro spy
  ([form]
   `(let [val# ~form]
      (log :debug ~(:line (meta &form)) '~form "=>" val#)
      val#))
  ([level form]
   `(let [val# ~form]
      (log ~level ~(:line (meta &form)) '~form "=>" val#)
      val#)))

(defn ^:private clean [data]
  (assoc data :hostname_ (delay nil)))

(log*/merge-config! {:level      (keyword (or (System/getenv "LOG_LEVEL")
                                              :info))
                     :middleware [clean]})
