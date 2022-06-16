(ns audiophile.exec.run
  (:require
    [audiophile.exec.shared :as shared]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.set :as set]
    [clojure.string :as string]))

(defmethod shared/main* :run
  [_ [mode & args]]
  (println "running with profile" (or (keyword mode) :dev))
  (shared/run* mode args))

(defmethod shared/run* :default
  [_ _]
  (shared/process! "docker-compose up -d --build app")
  (shared/process! "foreman start --procfile Procfile-dev"
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
  (try (shared/process! "docker-compose up -d rabbitmq postgres")
       (shared/process! (shared/clj #{:cljs-dev :test :shadow-cljs}
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
                               "-m audiophile.test.browser-runner")))

(defmethod shared/main* :seed
  [_ [file]]
  (let [sql (or file (str (System/getenv "PWD") "/dev/resources/db/seed.sql"))
        env-common (io/file ".env-common")
        env (if (.exists env-common)
              (-> env-common
                  slurp
                  edn/read-string
                  (select-keys #{"DB_USER" "DB_PASSWORD"})
                  (set/rename-keys {"DB_USER"     "PGUSER"
                                    "DB_PASSWORD" "PGPASSWORD"})
                  (assoc "PGHOST" "127.0.0.1"))
              {})]
    (shared/with-println [:db "seeding" "seeded"]
      (shared/process! (str "psql audiophile -f " sql)
                       env))))

(defmethod shared/main* :migrate
  [_ _]
  (shared/with-println [:db "migrating" "migrated"]
    (shared/process! (shared/clj #{:dev} "-m audiophile.backend.dev.migrations migrate"))))
