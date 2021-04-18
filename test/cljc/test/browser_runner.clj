(ns test.browser-runner
  (:require
    [immutant.web :as web]
    [ring.middleware.resource :as ring.res]
    [clojure.string :as string])
  (:import
    (org.openqa.selenium By)
    (org.openqa.selenium.firefox FirefoxDriver)))

(def ^:const ANSI_RESET "\u001B[0m")
(def ^:const ANSI_RED "\u001B[31m")
(def ^:const ANSI_GREEN "\u001B[32m")

(defn ready? [driver]
  (let [xs (reduce (fn [bldr el]
                     (.append bldr (.getText el)))
                   (StringBuilder.)
                   (.findElements driver (By/id "summary")))]
    (string/includes? (str xs) "Totals")))

(defn -main [& _]
  (let [env (System/getenv)
        server-port (Long/parseLong (or (get env "UI_TEST_PORT") "8080"))
        _ (web/run #(or (ring.res/resource-request % "private")
                        {:status 204})
                   {:port server-port :host "0.0.0.0"})
        driver (FirefoxDriver.)]
    (println "Running CLJS tests…")
    (.get driver (str "http://localhost:" server-port "/index.html"))
    (while (not (ready? driver))
      (Thread/sleep 1))
    (let [passes (count (.findElements driver (By/className "test-passing")))
          fails (count (.findElements driver (By/className "test-fail")))
          [color exit-code] (if (zero? fails)
                              [ANSI_GREEN 0]
                              [ANSI_RED 1])]
      (println "")
      (println (str color (+ passes fails) " tests. " passes " passed. " fails " failed." ANSI_RESET))

      (System/exit exit-code))))
