(ns com.ben-allred.audiophile.common.utils.logger
  #?(:cljs
     (:require-macros
       com.ben-allred.audiophile.common.utils.logger))
  (:require
    [clojure.pprint :as pp]
    [taoensso.timbre :as log*]))

(def ^:const ANSI_RESET "\u001B[0m")
(def ^:const ANSI_YELLOW "\u001B[33m")

(def ^:dynamic *ctx* nil)

(defn ^:private log* [form level args]
  `(when-not (:disabled? *ctx*) ;; TBD - ctx
     (log*/log! ~level :p ~args {:?line ~(:line (meta form))})))

(defn spy* [form level expr f separator]
  (let [sym (gensym)
        pr (gensym)]
    (list `let [sym expr
                pr `(~f ~sym)]
          (log* form level (list (str ANSI_YELLOW expr ANSI_RESET) separator pr))
          sym)))

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
   `(spy :info ~expr))
  ([level expr]
   (spy* &form level expr identity "=>")))

(defmacro spy-tap
  ([f expr]
   `(spy-tap :info ~f ~expr))
  ([level f expr]
   (spy* &form level expr f (str "(\uD83C\uDF7A " f ") =>"))))

(defmacro log [level line & args]
  `(when-not (:disabled? *ctx*) ;; TBD - ctx
     (log*/log! ~level :p ~args {:?line ~line})))

(defmacro spy-on
  ([f]
   `(spy-on :info ~f))
  ([level f]
   `(fn [& args#]
      (let [result# (apply ~f args#)]
        (when-not (:disabled? *ctx*)
          (log*/log! ~level :p (cons '~f args#) {:?line ~(:line (meta &form))}))
        result#))))

(defn pprint
  "Reagent component for displaying clojure data in the browser - debug only"
  [value]
  [:pre (with-out-str (pp/pprint value))])

(defn ^:private clean [data]
  (assoc data :hostname_ (delay nil)))

(log*/merge-config! {:level      (keyword (or #?(:clj (System/getenv "LOG_LEVEL"))
                                              :info))
                     :middleware [clean]})
