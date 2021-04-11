(ns com.ben-allred.audiophile.common.utils.logger
  (:require
    [taoensso.timbre :as log*]
    [clojure.string :as string]))

(defmacro debug [& args]
  `(log*/debug ~@args))

(defmacro info [& args]
  `(log*/info ~@args))

(defmacro warn [& args]
  `(log*/warn ~@args))

(defmacro error [& args]
  `(log*/error ~@args))

(defmacro spy [& args]
  `(log*/spy ~@args))

(defn output-fn
  ([data]
   (output-fn nil data))
  ([opts data]
   (let [{:keys [no-stacktrace?]} opts
         {:keys [level ?err msg_ ?ns-str ?file
                 timestamp_ ?line]} data]
     (str
       (some-> timestamp_ force) " "
       (string/upper-case (name level)) " "
       "[" (or ?ns-str ?file "?") ":" (or ?line "?") "] - "
       (force msg_)
       (when-not no-stacktrace?
         (when-let [err ?err]
           (str "\n" (log*/stacktrace err opts))))))))

(log*/merge-config! {:level     :debug
                     :output-fn output-fn})
