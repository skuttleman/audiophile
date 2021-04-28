(ns test.browser-runner
  (:require
    [clojure.string :as string]
    [com.ben-allred.audiophile.api.utils.ring :as ring]
    [com.ben-allred.audiophile.common.utils.maps :as maps]
    [immutant.web :as web])
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

(defn print-failures! [failures]
  (doseq [{:keys [ns var line context expected actual]} failures]
    (println "")
    (println (str RED "FAILURE" RESET))
    (println (str ns var))
    (println (str "at " CYAN line RESET))
    (println (str "  " context))
    (println (str "    EXPECTED: " expected))
    (println (str "    ACTUAL:   " GOLD actual RESET))))

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
      (let [server (web/run #(or (ring/resource-request % "private")
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
                failures (for [^WebElement el (by-css driver ".test-ns.has-failures")
                               :let [ns (text (by-css el "h2"))]
                               ^WebElement el (by-css el ".test-var.has-failures")
                               :let [line (text (by-css el ".test-var-line"))
                                     var (-> (by-css el ".var-header")
                                             text
                                             (string/replace line "")
                                             string/trim)]
                               ^WebElement el (by-css el ".test-fail")
                               :let [context (text (by-css el ".contexts"))
                                     [expected actual] (texts (by-css el "pre > code"))]]
                           (maps/->m ns var line context expected actual))
                [color code] (if (zero? fails)
                               [GREEN 0]
                               [RED 1])]
            (vreset! exit-code code)
            (print-failures! failures)
            (println "")
            (println (str color tests " tests. " passes " passed. " fails " failed." RESET)))
          (finally
            (.quit driver)
            (web/stop server))))
      (finally
        (System/exit @exit-code)))))
