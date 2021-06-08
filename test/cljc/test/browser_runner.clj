(ns test.browser-runner
  (:require
    [clojure.string :as string]
    [com.ben-allred.audiophile.backend.infrastructure.http.ring :as ring]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [immutant.web :as web*])
  (:import
    (org.openqa.selenium By SearchContext WebElement)
    (org.openqa.selenium.firefox FirefoxDriver)))

(def ^:const RESET "\u001B[0m")
(def ^:const RED "\u001B[31m")
(def ^:const GREEN "\u001B[32m")
(def ^:const CYAN "\u001B[36m")
(def ^:const GOLD "\u001B[33;1m")

(defn ready? [driver]
  (let [xs (reduce (fn [bldr el]
                     (.append bldr (.getText el)))
                   (StringBuilder.)
                   (.findElements driver (By/id "summary")))]
    (string/includes? (str xs) "Totals")))

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

(defn ^:private by-css [^SearchContext element selector]
  (.findElements element (By/cssSelector selector)))

(defn ^:private texts [elements]
  (map #(.getText %) elements))

(defn ^:private text [elements]
  (string/join "\n" (texts elements)))

(defn -main [& _]
  (let [env (System/getenv)
        exit-code (volatile! 1)
        server-port (Long/parseLong (or (get env "UI_TEST_PORT") "8080"))]
    (try
      (let [server (web*/run #(or (ring/resource-request % "private")
                                  {:status 204})
                             {:port server-port :host "0.0.0.0"})
            driver (FirefoxDriver.)]
        (try
          (println "Running CLJS tests…")
          (.get driver (str "http://localhost:" server-port "/index.html"))
          (while (not (ready? driver))
            (Thread/sleep 1))
          (let [tests (count (by-css driver ".test-var"))
                passes (count (by-css driver ".test-passing"))
                fails (count (by-css driver ".test-fail"))
                failures (for [^WebElement el (concat (by-css driver ".test-ns.has-failures")
                                                      (by-css driver ".test-ns.has-errors"))
                               :let [ns (text (by-css el "h2"))]
                               [type ^WebElement el] (concat (map (partial vector :failure)
                                                                  (by-css el ".test-var.has-failures"))
                                                             (map (partial vector :error)
                                                                  (by-css el ".test-var.has-errors")))
                               :let [line (text (by-css el ".test-var-line"))
                                     var (-> (by-css el ".var-header")
                                             text
                                             (string/replace line "")
                                             string/trim)]
                               ^WebElement el (by-css el (case type
                                                           :failure ".test-fail:not(.test-error)"
                                                           :error ".test-error"))
                               :let [context (text (concat (by-css el ".contexts")
                                                           (by-css el ".error-message")))
                                     [expected actual] (texts (by-css el "pre > code"))]]
                           (maps/->m type ns var line context expected actual))
                [color code] (if (zero? fails)
                               [GREEN 0]
                               [RED 1])]
            (vreset! exit-code code)
            (print-failures! failures)
            (println "")
            (println (str color tests " tests. " passes " passed. " fails " failed." RESET)))
          (finally
            (.quit driver)
            (web*/stop server))))
      (finally
        (System/exit @exit-code)))))
