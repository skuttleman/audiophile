(ns audiophile.exec.run
  (:require
    [audiophile.exec.shared :as shared]
    [babashka.process :as p]
    [clojure.string :as string]))

(defmethod shared/main* :run
  [_ [mode & args]]
  (println "running with profile" (or (keyword mode) :single))
  (shared/run* mode args))

(defmethod shared/run* :jar
  [_ _]
  (shared/main* :clean nil)
  (shared/main* :build nil)
  (shared/process! "foreman start"
                   (-> {"LOG_LEVEL" "info"}
                       shared/with-default-env
                       (assoc "ENV" "production"
                              "SERVICES" "api auth jobs ui"))))

(defmethod shared/run* :split
  [_ _]
  (shared/process! "foreman start --procfile Procfile-split"
                   (-> {"LOG_LEVEL" "debug"}
                       shared/with-default-env
                       (assoc "ENV" "development"
                              "WS_RECONNECT_MS" "1000"))))

(defmethod shared/run* :multi
  [_ _]
  (shared/process! "foreman start --procfile Procfile-multi"
                   (-> {"LOG_LEVEL" "debug"}
                       shared/with-default-env
                       (assoc "ENV" "development"
                              "WS_RECONNECT_MS" "1000"))))

(defmethod shared/run* :default
  [_ _]
  (shared/process! "foreman start --procfile Procfile-single"
                   (-> {"LOG_LEVEL" "debug"}
                       shared/with-default-env
                       (assoc "ENV" "development"
                              "WS_RECONNECT_MS" "1000"))))

(defmethod shared/main* :test
  [_ [mode & args]]
  (if mode
    (shared/test* mode args)
    (doseq [mode (->> (methods shared/test*)
                      keys
                      (concat [:clj :cljs])
                      distinct)]
      (shared/test* mode nil))))

(defmethod shared/test* :clj
  [_ args]
  (try (shared/process! (shared/clj #{:cljs-dev :test :shadow-cljs}
                                    "-m shadow.cljs.devtools.cli compile web-test"))
       (shared/process! (shared/clj #{:dev :test} (str "-m kaocha.runner"
                                                       (when (seq args)
                                                         (str " " (string/join " " args))))))
       (finally
         (shared/main* :wipe ["rabbit" "all" "test"]))))

(defmethod shared/test* :cljs
  [_ _]
  (shared/process! (shared/clj #{:cljs-dev :test :shadow-cljs}
                               "-m shadow.cljs.devtools.cli compile test"))
  (shared/process! (shared/clj #{:dev :test}
                               "-m com.ben-allred.audiophile.test.browser-runner")))

(defmethod shared/main* :seed
  [_ [file]]
  (let [sql (or file (str (System/getenv "PWD") "/dev/resources/db/seed.sql"))]
    (shared/with-println [:db "seeding" "seeded"]
      (shared/process! (str "psql audiophile -f " sql)))))

(defmethod shared/main* :migrate
  [_ _]
  (shared/with-println [:db "migrating" "migrated"]
    (shared/process! #{:dev} "-m com.ben-allred.audiophile.backend.dev.migrations migrate")))
