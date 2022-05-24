(ns audiophile.common.core.utils.logger
  #?(:cljs
     (:require-macros
       audiophile.common.core.utils.logger))
  (:require
    [clojure.pprint :as pp]
    [clojure.string :as string]
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
    `(let [ctx# ~ctx]
       (binding [*ctx* (merge *ctx*
                              {:logger/class nil
                               :logger/id    nil
                               :logger/name  nil}
                              (cond
                                (map? ctx#) ctx#
                                (vector? ctx#) {:logger/class (first ctx#)
                                                :logger/id    (second ctx#)}
                                (keyword? ctx#) {:logger/id ctx#}
                                :else {:logger/class ctx#}))]
         ~@body))))

(defmacro log [level & args]
  (log* &form level args))

(defmacro trace [& args]
  (log* &form :trace args))

(defmacro debug [& args]
  (log* &form (if (:ns &env) :info :debug) args))

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

#?(:cljs
   (defn pprint
     "Reagent component for displaying clojure data in the browser - debug only"
     [value]
     [:pre (with-out-str (pp/pprint value))]))

(defn ^:private filter-external-packages [{:keys [level ?ns-str] :as data}]
  (when (or (#{:warn :error :fatal :report} level)
            (some-> ?ns-str (string/starts-with? "audiophile")))
    data))

(defn ^:private output-fn [{:keys [level ?err msg_ ?ns-str ?file timestamp_ ?line]}]
  (let [logger (or (:logger/name *ctx*)
                   #?(:clj (some-> *ctx* :logger/class class .getSimpleName)))
        loc (str (when logger "\u001B[38;5;230m")
                 (or ?ns-str ?file "?")
                 (when logger (str "/" logger "\u001B[0m"))
                 ":"
                 (or ?line "?"))]
    (str
      (when-let [ts (some-> timestamp_ deref)]
        (str ts " "))
      (string/upper-case (name level))
      " [" loc "\u001B[0m]"
      (when-let [ctx (:logger/id *ctx*)]
        (str " \u001b[35;1m" (name ctx) "\u001b[0m"))
      (when-let [ctx (:request/id *ctx*)]
        (str " |\u001b[34;1m" ctx "\u001b[0m|"))
      ": "
      @msg_
      (some-> ?err log*/stacktrace))))

(log*/merge-config! {:level      (keyword (or #?(:clj (System/getenv "LOG_LEVEL"))
                                              :info))
                     :middleware [filter-external-packages]
                     :output-fn  output-fn})
