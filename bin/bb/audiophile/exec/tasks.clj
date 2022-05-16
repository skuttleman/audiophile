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
                      (concat [:cljs :clj])
                      distinct)]
      (shared/build* mode nil)) ))

(defmethod shared/build* :clj
  [_ _]
  (shared/with-println [:uberjar "building" "built"]
    (shared/process! "rm -rf classes")
    (shared/process! "mkdir classes")
    (shared/process! "rm -f target/audiophile.jar")
    (shared/process! (shared/clj "-e \"(compile 'com.ben-allred.audiophile.backend.core)\""))
    (shared/process! (shared/clj #{:uberjar}
                                 ["-m"
                                  "uberdeps.uberjar"
                                  "--level"
                                  "warn"
                                  "--target"
                                  "target/audiophile.jar"
                                  "--main-class"
                                  "com.ben_allred.audiophile.backend.core"])
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
  [_ [mode & args]]
  (if mode
    (shared/clean* mode args)
    (doseq [[mode] (methods shared/clean*)]
      (shared/clean* mode nil))))

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

(defmethod shared/main* :docker
  [_ _]
  (shared/main* :build nil)
  (shared/with-println [:docker:latest "building" "built"]
    (shared/process! "docker build -t audiophile -f Dockerfile .")
    (shared/process! "docker tag audiophile skuttleman/audiophile:latest")
    (shared/process! "docker push skuttleman/audiophile:latest"))

  (shared/with-println [:docker:dev "building" "built"]
    (shared/process! "docker build -t audiophile-dev -f Dockerfile-dev .")
    (shared/process! "docker tag audiophile-dev skuttleman/audiophile:dev")
    (shared/process! "docker push skuttleman/audiophile:dev")))

(defmethod shared/main* :install
  [_ _]
  (shared/with-println [:repo "installing" "installed"]
    (shared/process! "cp bin/pre-commit.sh .git/hooks/pre-commit")
    (shared/process! "npm install")
    (shared/process! "clj -X:deps prep")))

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
    (shared/with-println [:rabbitmq "deleting" "deleted"]
      (doseq [name (-> (curl/get url)
                       :body
                       (cheshire/parse-string true)
                       (->> (map :name)))
              :when (re-find (re-pattern (format "(audiophile%s)"
                                                 (or (some->> ext (str "\\."))
                                                     "")))
                             name)]
        (println "deleting" name)
        (shared/process! (format "rabbitmqadmin delete %s name=%s" single name))))))

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
    (shared/with-println [:postgres.data "deleting" "deleted"]
      (shared/process! (format "psql %s -c \"%s\"" db query)))))

(defmethod shared/wipe* :artifacts
  [_ _]
  (shared/with-println [:artifacts "deleting" "deleted"]
    (shared/process! "/bin/rm -f target/artifacts/*")))
