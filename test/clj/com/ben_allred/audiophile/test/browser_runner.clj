(ns com.ben-allred.audiophile.test.browser-runner
  (:require
    [clojure.pprint :as pp]
    [clojure.string :as string]
    [com.ben-allred.audiophile.backend.infrastructure.http.ring :as ring]
    [com.ben-allred.audiophile.common.core.utils.core :as u]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.audiophile.test.utils.selenium :as selenium]
    [immutant.web :as web*])
  (:import
    (org.apache.commons.io.output NullWriter)))

(def ^:const RESET "\u001B[0m")
(def ^:const RED "\u001B[31m")
(def ^:const GREEN "\u001B[32m")
(def ^:const CYAN "\u001B[36m")
(def ^:const GOLD "\u001B[33;1m")

(defn ready? [ctx]
  (string/includes? (->> "summary"
                         selenium/by-id
                         (selenium/find-by ctx)
                         selenium/text)
                    "Totals"))

(defmulti print-failure! :type)

(defmethod print-failure! :failure
  [{:keys [ns var line context expected actual]}]
  (println "")
  (println (str RED "FAILURE" RESET))
  (println (str ns var))
  (println (str "at " CYAN line RESET))
  (println (str "  " context))
  (println (str "    EXPECTED: " expected))
  (println (str "    ACTUAL:   " GOLD actual RESET)))

(defmethod print-failure! :error
  [{:keys [ns var line context]}]
  (println "")
  (println (str RED "ERROR" RESET))
  (println (str ns var))
  (println (str "at " CYAN line RESET))
  (println (str "  " GOLD context RESET)))

(defn print-failures! [failures]
  (doseq [failure failures]
    (print-failure! failure)))

(defn ^:private find-by-css
  ([ctx selector]
   (find-by-css ctx selector identity))
  ([ctx selector mapper]
   (->> selector
        selenium/by-css
        (selenium/find-by ctx)
        (map mapper))))

(def ^:private text-by-css
  (comp selenium/text find-by-css))

(defn ^:private test-elements
  ([driver]
   (test-elements driver identity identity))
  ([driver failure-mapper error-mapper]
   (concat (find-by-css driver ".test-ns.has-failures" failure-mapper)
           (find-by-css driver ".test-ns.has-errors" error-mapper))))

(defn ^:private ->results [driver]
  (for [el (test-elements driver)
        :let [ns (text-by-css el "h2")]
        [type el] (test-elements driver
                                 (partial vector :failure)
                                 (partial vector :error))
        :let [line (text-by-css el ".test-var-line")
              var (-> el
                      (text-by-css ".var-header")
                      (string/replace line "")
                      string/trim)]
        el (find-by-css el (case type
                             :failure ".test-fail:not(.test-error)"
                             :error ".test-error"))
        :let [context (->> ".contexts"
                           (find-by-css el)
                           (concat (find-by-css el ".error-message"))
                           selenium/text)
              [expected actual] (->> "pre > code"
                                     (find-by-css el)
                                     selenium/texts)]]
    (maps/->m type ns var line context expected actual)))

(defn ^:private run-tests [driver server-port]
  (println "Running CLJS tests…")
  (selenium/visit! driver (str "http://localhost:" server-port "/index.html"))
  (selenium/wait-for! driver ready? 5000)
  (let [tests (count (selenium/find-by driver (selenium/by-css ".test-var")))
        passes (count (selenium/find-by driver (selenium/by-css ".test-passing")))
        fails (count (selenium/find-by driver (selenium/by-css ".test-fail")))
        failures (doall (->results driver))
        [color code] (if (zero? fails)
                       [GREEN 0]
                       [RED 1])]
    {:exit-code code
     :results   (maps/->m failures color tests passes fails)}))

(defn -main [& _]
  (let [env (System/getenv)
        results (volatile! {:exit-code 1})
        server-port (Long/parseLong (or (get env "UI_TEST_PORT") "8080"))]
    (try
      (let [server (web*/run #(or (ring/resource-request % "private")
                                  {:status 204})
                             {:port server-port :host "0.0.0.0"})
            driver (selenium/create-driver nil)]
        (try
          (vreset! results (run-tests driver server-port))
          (finally
            (binding [*out* NullWriter/NULL_WRITER]
              (u/silent!
                (web*/stop server)
                (.quit driver))))))
      (catch Throwable ex
        (pp/pprint ex))
      (finally
        (when-let [{:keys [color fails failures passes tests]} (:results @results)]
          (print-failures! failures)
          (println "")
          (println (str color tests " tests. " passes " passed. " fails " failed." RESET)))
        (System/exit (:exit-code @results))))))
