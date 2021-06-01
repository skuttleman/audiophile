(ns com.ben-allred.audiophile.common.core.utils.logger
  #?(:cljs
     (:require-macros
       com.ben-allred.audiophile.common.core.utils.logger))
  (:require
    [clojure.pprint :as pp]
    [taoensso.timbre :as log*]))

(def ^:const ANSI_RESET "\u001B[0m")
(def ^:const ANSI_YELLOW "\u001B[33m")
(def ^:const ANSI_GREEN "\u001B[32;1m")

(def ^:dynamic *ctx* nil)

(defn ^:private log* [form level args]
  `(when-not (:disabled? *ctx*) ;; TBD - ctx
     (log*/log! ~level :p ~args {:?line ~(:line (meta form))})))

(defn spy* [form level expr f separator]
  (let [pr (gensym "pr")]
    `(let [val# ~expr
           ~pr (~f val#)]
       ~(log* form level (list (str ANSI_YELLOW expr ANSI_RESET) separator pr))
       val#)))

(defmacro with-ctx [ctx & body]
  (if (:ns &env)
    `(do ~@body)
    `(binding [*ctx* (merge *ctx* ~ctx)]
       ~@body)))

(defmacro trace [& args]
  (log* &form :trace args))

(defmacro debug [& args]
  (log* &form :debug args))

(defmacro info [& args]
  (log* &form :info args))

(defmacro warn [& args]
  (log* &form :warn args))

(defmacro error [& args]
  (log* &form :error args))

(defmacro fatal [& args]
  (log* &form :fatal args))

(defmacro report [& args]
  (log* &form :report args))

(defmacro spy
  ([expr]
   (spy* &form :info expr `identity "=>"))
  ([level expr]
   (spy* &form level expr `identity "=>"))
  ([level msg expr]
   (let [pr (gensym "pr")]
     `(let [~pr ~expr]
        ~(log* &form level (list `(str ~ANSI_GREEN ~msg ~ANSI_RESET) "=>" pr))
        ~pr))))

(defmacro spy-tap
  ([f expr]
   (spy* &form :info expr f (str "(\uD83C\uDF7A " f ") =>")))
  ([level f expr]
   (spy* &form level expr f (str "(\uD83C\uDF7A " f ") =>"))))

(defn pprint
  "Reagent component for displaying clojure data in the browser - debug only"
  [value]
  [:pre (with-out-str (pp/pprint value))])

(defn ^:private clean [data]
  (assoc data :hostname_ (delay nil)))

(log*/merge-config! {:level      (keyword (or #?(:clj (System/getenv "LOG_LEVEL"))
                                              :info))
                     :middleware [clean]})
