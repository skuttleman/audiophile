(ns audiophile.exec.tasks
  (:require
    [audiophile.exec.shared :as shared]
    [babashka.curl :as curl]
    [cheshire.core :as cheshire]
    [clojure.string :as string]))

(defmethod shared/main* :build
  [_ [mode & args]]
  (if mode
    (shared/build* mode args)
    (doseq [mode (->> (methods shared/build*)
                      keys
                      (concat [:clj :cljs])
                      distinct)]
      (shared/build* mode nil)) ))

(defmethod shared/build* :clj
  [_ _]
  (println ["building" "uberjar" "…"])
  (shared/process! "rm -rf classes")
  (shared/process! "mkdir classes")
  (shared/process! "rm -f target/audiophile.jar")
  (shared/process! (shared/clj "-e (compile 'com.ben-allred.audiophile.backend.core)"))
  (shared/process! (shared/clj #{:uberjar}
                               ["-m"
                                "uberdeps.uberjar"
                                "--level"
                                "warn"
                                "--target"
                                "target/audiophile.jar"
                                "--main-class"
                                "com.ben_allred.audiophile.backend.core"])
                   {"LOG_LEVEL" "warn"})
  (println ["…" "uberjar" "built"]))

(defmethod shared/build* :cljs
  [_ _]
  (println ["building" "ui" "…"])
  (shared/process! "npm install")
  (shared/process! "rm -rf resources/public/css")
  (shared/process! "sass --style=compressed src/scss/main.scss resources/public/css/main.css")
  (shared/process! "rm -rf resources/public/js")
  (shared/process! (shared/clj #{:shadow-cljs}
                               "-m shadow.cljs.devtools.cli compile ui"))
  (println ["…" "ui" "built"]))

(defmethod shared/main* :clean
  [_ [mode & args]]
  (if mode
    (shared/clean* mode args)
    (doseq [[mode] (methods shared/clean*)]
      (shared/clean* mode nil))))

(defmethod shared/clean* :clj
  [_ _]
  (println ["cleaning" "clj" "…"])
  (shared/process! "rm -rf .cpcache classes target/audiophile.jar")
  (println ["…" "clj" "clean"]))

(defmethod shared/clean* :cljs
  [_ _]
  (println ["cleaning" "cljs" "…"])
  (shared/process! "rm -rf .shadow-cljs resources/public/css resources/public/js")
  (println ["…" "cljs" "clean"]))

(defmethod shared/main* :deploy
  [_ _]
  (println ["deploying" "…"])
  (shared/process! "heroku deploy:jar target/audiophile.jar --app skuttleman-audiophile")
  (println ["…" "deployed"]))

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

(defmethod shared/main* :docker
  [_ _]
  (shared/main* :build nil)
  (println ["building" "docker" ":latest" "…"])
  (shared/process! "docker build -t audiophile -f Dockerfile .")
  (shared/process! "docker tag audiophile skuttleman/audiophile:latest")
  (shared/process! "docker push skuttleman/audiophile:latest")
  (println ["…" "docker" ":latest" "built"])

  (println ["building" "docker" ":dev" "…"])
  (shared/process! "docker build -t audiophile-dev -f Dockerfile-dev .")
  (shared/process! "docker tag audiophile-dev skuttleman/audiophile:dev")
  (shared/process! "docker push skuttleman/audiophile:dev")
  (println ["…" "docker" ":dev" "built"]))

(defmethod shared/main* :install
  [_ _]
  (println ["installing" "repo" "…"])
  (shared/process! "cp bin/pre-commit.sh .git/hooks/pre-commit")
  (shared/process! "npm install")
  (shared/process! "clj -X:deps prep")
  (println ["…" "repo" "installed"]))

(defmethod shared/main* :wipe
  [_ [mode & args]]
  (if mode
    (shared/wipe* mode args)
    (doseq [[mode] (methods shared/wipe*)]
      (shared/wipe* mode nil))))

(defmethod shared/wipe* :rabbit
  [_ [mode & args]]
  (case mode
    (nil "all") (doseq [mode (->> (methods shared/rabbit*)
                                  keys
                                  (concat [:queues :exchanges])
                                  distinct)]
                  (shared/rabbit* mode args))
    (shared/rabbit* mode args)))

(defn ^:private rabbit* [single plural ext]
  (let [url (str "http://guest:guest@localhost:15672/api/" plural)]
    (println ["deleting" "rabbitmq" plural "…"])
    (doseq [name (-> (curl/get url)
                     :body
                     (cheshire/parse-string true)
                     (->> (map :name)))
            :when (re-find (re-pattern (format "(audiophile%s)"
                                               (or (some->> ext (str "\\."))
                                                   "")))
                           name)]
      (println "deleting" name)
      (shared/process! (format "rabbitmqadmin delete %s name=%s" single name)))
    (println ["…" "rabbitmq" plural "deleted"])))

(defmethod shared/rabbit* :exchanges
  [_ [ext]]
  (rabbit* "exchange" "exchanges" ext))

(defmethod shared/rabbit* :queues
  [_ [ext]]
  (rabbit* "queue" "queues" ext))

(defmethod shared/wipe* :psql
  [_ [db]]
  (let [db (or db "audiophile")
        query (string/join ";"
                           ["BEGIN"
                            "TRUNCATE projects CASCADE"
                            "TRUNCATE events CASCADE"
                            "TRUNCATE users CASCADE"
                            "TRUNCATE teams CASCADE"
                            "DELETE FROM artifacts"
                            "COMMIT"])]
    (println ["deleting" "postgres" "data" "…"])
    (shared/process! (format "psql %s -c \"%s\"" db query))
    (println ["…" "postgres" "data" "deleted"])))

(defmethod shared/wipe* :artifacts
  [_ _]
  (println ["deleting" "artifacts" "…"])
  (shared/process! "/bin/rm -f target/artifacts/*")
  (println ["…" "artifacts" "deleted"]))
