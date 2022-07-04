(ns audiophile.exec.tasks
  (:require
    [audiophile.exec.shared :as shared]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.set :as set]
    [clojure.string :as string]))

(defmethod shared/main* :build
  [_ args]
  (shared/multi* shared/build* args (->> (methods shared/build*)
                                         keys
                                         (concat [:cljs :clj]))))

(defmethod shared/build* :clj
  [_ _]
  (shared/with-println [:uberjar "building" "built"]
    (shared/process! "rm -rf classes")
    (shared/process! "mkdir classes")
    (shared/process! "rm -f target/audiophile.jar")
    (shared/process! (shared/clj "-e \"(compile 'audiophile.backend.core)\""))
    (shared/process! (shared/clj #{:uberjar}
                                 ["-m"
                                  "uberdeps.uberjar"
                                  "--level"
                                  "warn"
                                  "--target"
                                  "target/audiophile.jar"
                                  "--main-class"
                                  "audiophile.backend.core"])
                     {"LOG_LEVEL" "warn"})))

(defmethod shared/build* :cljs
  [_ _]
  (shared/with-println [:ui "building" "built"]
    (shared/process! "npm install")
    (shared/process! "rm -rf resources/public/css")
    (shared/process! "sass --style=compressed src/scss/main.scss resources/public/css/main.css")
    (shared/process! "rm -rf resources/public/js")
    (shared/process! (shared/clj #{:shadow-cljs}
                                 "-m shadow.cljs.devtools.cli compile ui"))))

(defmethod shared/main* :clean
  [_ args]
  (shared/multi* shared/clean* args))

(defmethod shared/clean* :clj
  [_ _]
  (shared/with-println [:clj "cleaning" "cleaned"]
    (shared/process! "rm -rf .cpcache classes target/audiophile.jar")))

(defmethod shared/clean* :cljs
  [_ _]
  (shared/with-println [:cljs "cleaning" "cleaned"]
    (shared/process! "rm -rf .shadow-cljs resources/public/css resources/public/js")))

(defmethod shared/main* :deploy
  [_ _]
  (shared/with-println [:app "deploying" "deployed"]
    (shared/process! "heroku deploy:jar target/audiophile.jar --app skuttleman-audiophile")))

(defn ^:private main* [[mode & args]]
  (shared/main* mode (remove string/blank? args)))

(defmethod shared/main* :do
  [_ args]
  (loop [run [] [arg :as args] args]
    (cond
      (empty? args) (some-> run seq main*)
      (string/ends-with? arg ",") (do (main* (conj run (subs arg 0 (dec (count arg)))))
                                      (println "")
                                      (recur [] (rest args)))
      :else (recur (conj run arg) (rest args)))))

(defmethod shared/main* :install
  [_ args]
  (shared/multi* shared/install* args))

(defmethod shared/install* :repo
  [_ _]
  (shared/with-println [:repo "installing" "installed"]
    (shared/process! "cp bin/pre-commit.sh .git/hooks/pre-commit")
    (shared/process! "npm install")
    (shared/process! "clj -X:deps prep")))

(defmethod shared/install* :kafka
  [_ _]
  (shared/with-println [:kafka "installing" "installed"]
    (shared/process! "docker-compose up -d kafka")
    (doseq [topic ["spigot-events" "spigot-tasks" "spigot-workflows"]]
      (shared/silent! (-> "docker-compose exec -- kafka kafka-topics.sh"
                          (shared/with-opts {:create             true
                                             :topic              topic
                                             :bootstrap-server   "127.0.0.1:9092"
                                             :partitions         10
                                             :replication-factor 1})
                          (shared/process!))))))

(defmethod shared/main* :stop
  [_ args]
  (shared/multi* shared/stop* args))

(defmethod shared/stop* :app
  [_ _]
  (shared/with-println [:app "stopping" "stopped"]
    (shared/process! "docker-compose stop app")
    (shared/process! "docker-compose rm -fv")))

(defmethod shared/main* :wipe
  [_ args]
  (shared/multi* shared/wipe* args))

(defmethod shared/wipe* :psql
  [_ [db]]
  (let [db (or db "audiophile")
        query (string/join ";"
                           ["BEGIN"
                            "TRUNCATE projects CASCADE"
                            "TRUNCATE events CASCADE"
                            "TRUNCATE users CASCADE"
                            "TRUNCATE teams CASCADE"
                            "TRUNCATE workflows CASCADE"
                            "DELETE FROM artifacts"
                            "COMMIT"])
        env-common (io/file ".env-common")
        env (if (.exists env-common)
              (-> env-common
                  slurp
                  edn/read-string
                  (assoc "DB_HOST" "localhost")
                  (set/rename-keys {"DB_HOST"     "PGHOST"
                                    "DB_USER"     "PGUSER"
                                    "DB_PASSWORD" "PGPASSWORD"}))
              {})]
    (shared/with-println [:postgres.data "deleting" "deleted"]
      (shared/process! (format "psql %s -c \"%s\"" db query)
                       env))))

(defmethod shared/wipe* :artifacts
  [_ _]
  (shared/with-println [:artifacts "deleting" "deleted"]
    (shared/process! "/bin/rm -f target/artifacts/*")))

(defmethod shared/wipe* :kafka
  [_ _]
  (shared/with-println [:kafka "deleting" "deleted"]
    (shared/process! "docker-compose stop kafka zookeeper app")
    (shared/process! "docker-compose rm -fv")
    (shared/silent! (shared/process! "docker volume rm audiophile_kafka_data"))
    (shared/silent! (shared/process! "docker volume rm audiophile_zookeeper_data")))
  (shared/install* :kafka nil))
