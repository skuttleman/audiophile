(ns ^:unit audiophile.backend.infrastructure.pubsub.handlers.files-test
  (:require
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.backend.infrastructure.db.files :as db.files]
    [audiophile.backend.infrastructure.db.models.sql :as sql]
    [audiophile.backend.infrastructure.pubsub.handlers.files :as pub.files]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils :as tu]
    [audiophile.test.utils.repositories :as trepos]
    [audiophile.test.utils.services :as ts]
    [audiophile.test.utils.stubs :as stubs]
    [clojure.test :refer [are deftest is testing]]))

(deftest handle!-test
  (testing "(FileCommandHandler#handle!)"
    (let [ch (ts/->chan)
          tx (trepos/stub-transactor db.files/->FilesRepoExecutor)
          handler (pub.files/->FileCommandHandler tx ch)]
      (testing "when saving artifacts"
        (testing "and when the content saves to the kv-store"
          (let [[artifact-id user-id request-id] (repeatedly uuids/random)
                artifact {:artifact/id       artifact-id
                          :artifact/uri      "some-uri"
                          :artifact/filename "file.name"}]
            (stubs/use! tx :execute!
                        [{:id artifact-id}]
                        [artifact]
                        [{:id "event-id"}])
            (int/handle! handler
                         {:command/type :artifact/create!
                          :command/data {:artifact/filename     "file.name"
                                         :artifact/size         12345
                                         :artifact/content-type "content/type"
                                         :artifact/uri          "some://uri"}
                          :command/ctx  {:user/id    user-id
                                         :request/id request-id}})

            (let [[[insert-artifact] [query-artifact]] (colls/only! 2 (stubs/calls tx :execute!))]
              (testing "inserts the artifact in the repository"
                (is (= {:insert-into :artifacts
                        :values      [{:filename     "file.name"
                                       :content-type "content/type"
                                       :uri          "some://uri"
                                       :content-length 12345}]
                        :returning   [:id]}
                       insert-artifact)))

              (testing "queries the event payload"
                (let [{:keys [select from where]} query-artifact]
                  (is (= #{[:artifacts.filename "artifact/filename"]
                           [:artifacts.id "artifact/id"]
                           [:artifacts.content-type "artifact/content-type"]
                           [:artifacts.uri "artifact/uri"]
                           [:artifacts.content-length "artifact/content-length"]
                           [:artifacts.created-at "artifact/created-at"]
                           [:artifacts.key "artifact/key"]}
                         (set select)))
                  (is (= [:artifacts] from))
                  (is (= [:= #{:artifacts.id artifact-id}]
                         (tu/op-set where)))))

              (testing "emits an event"
                (let [{event-id :event/id :as event} (-> ch
                                                         (stubs/calls :send!)
                                                         colls/only!
                                                         first)]
                  (is (uuid? event-id))
                  (is (= {:event/id         event-id
                          :event/type       :artifact/created
                          :event/model-id   artifact-id
                          :event/data       artifact
                          :event/emitted-by user-id
                          :event/ctx        {:user/id    user-id
                                             :request/id request-id}}
                         event)))))))

        (testing "and when the executor throws an exception"
          (let [request-id (uuids/random)
                user-id (uuids/random)]
            (stubs/init! ch)
            (stubs/use! tx :execute!
                        (ex-info "Executor" {}))
            (int/handle! handler
                         {:command/type :artifact/create!
                          :command/data {}
                          :command/ctx  {:user/id    user-id
                                         :request/id request-id}})

            (testing "does not emit a successful event"
              (empty? (stubs/calls ch :publish!)))

            (testing "emits a command-failed event"
              (let [{event-id :event/id :as event} (-> ch
                                                       (stubs/calls :send!)
                                                       colls/only!
                                                       first)]
                (is (uuid? event-id))
                (is (= {:event/id         event-id
                        :event/model-id   request-id
                        :event/type       :command/failed
                        :event/data       {:error/command :artifact/create!
                                           :error/reason  "Executor"}
                        :event/emitted-by user-id
                        :event/ctx        {:request/id request-id
                                           :user/id    user-id}}
                       event))))))

        (testing "and when the pubsub throws an exception"
          (let [request-id (uuids/random)
                user-id (uuids/random)]
            (stubs/init! ch)
            (stubs/use! ch :send!
                        (ex-info "Channel" {}))
            (int/handle! handler
                         {:command/type :artifact/create!
                          :command/data {}
                          :command/ctx  {:user/id    user-id
                                         :request/id request-id}})

            (testing "emits a command-failed event"
              (let [{event-id :event/id :as event} (-> ch
                                                       (stubs/calls :send!)
                                                       rest
                                                       colls/only!
                                                       first)]
                (is (uuid? event-id))
                (is (= {:event/id         event-id
                        :event/model-id   request-id
                        :event/type       :command/failed
                        :event/data       {:error/command :artifact/create!
                                           :error/reason  "Channel"}
                        :event/emitted-by user-id
                        :event/ctx        {:request/id request-id
                                           :user/id    user-id}}
                       event)))))))

      (testing "when saving files"
        (let [[artifact-id file-id project-id user-id] (repeatedly uuids/random)
              file {:file/id         file-id
                    :file/name       "file name"
                    :file/project-id project-id}
              ch (ts/->chan)
              tx (trepos/stub-transactor db.files/->FilesRepoExecutor)
              handler (pub.files/->FileCommandHandler tx ch)]
          (testing "and when creating a file"
            (stubs/use! tx :execute!
                        [{:id project-id}]
                        [{:id file-id}]
                        nil
                        [file]
                        [{:id "event-id"}])
            (int/handle! handler
                         {:command/type :file/create!
                          :command/data {:file/name    "file name"
                                         :version/name "version"
                                         :artifact/id  artifact-id}
                          :command/ctx  {:user/id    user-id
                                         :project/id project-id}})

            (let [[[access-query] [file-insert] [version-insert] [query-for-event]]
                  (colls/only! 4 (stubs/calls tx :execute!))]
              (testing "checks for access permission"
                (is (= [:projects] (:from access-query)))
                (is (= [:and
                        #{[:= #{:user-teams.user-id user-id}]
                          [:= #{:projects.id project-id}]}]
                       (-> access-query
                           :where
                           (update 1 tu/op-set)
                           (update 2 tu/op-set)
                           tu/op-set)))
                (is (= [:user-teams [:= #{:user-teams.team-id :projects.team-id}]]
                       (-> access-query
                           :join
                           (update 1 tu/op-set)))))

              (testing "saves file data to the repository"
                (is (= :files (:insert-into file-insert)))
                (is (= #{:id} (set (:returning file-insert))))
                (let [value (colls/only! (:values file-insert))]
                  (is (= "file name" (:name value)))
                  (is (= project-id (:project-id value)))
                  (let [query (:idx value)
                        {:keys [select from where]} query]
                    (is (= #{(sql/count :idx)} (set select)))
                    (is (= [:files] from))
                    (is (= [:= #{:files.project-id project-id}]
                           (tu/op-set where))))))

              (testing "saves version data to the repository"
                (is (= {:insert-into :file-versions
                        :values      [{:artifact-id artifact-id
                                       :file-id     file-id
                                       :name        "version"}]
                        :returning   #{:id}}
                       (update version-insert :returning set))))

              (testing "queries a file from the repository"
                (let [{:keys [select from where join]} query-for-event]
                  (is (= #{[:files.name "file/name"]
                           [:files.id "file/id"]
                           [:files.idx "file/idx"]
                           [:files.project-id "file/project-id"]
                           [:version.created-at "version/created-at"]
                           [:fv.id "version/id"]
                           [:fv.name "version/name"]
                           [:fv.artifact-id "version/artifact-id"]}
                         (set select)))
                  (is (= [:files] from))
                  (is (= [:= #{:files.id file-id}] (tu/op-set where)))
                  (is (= [[{:select   #{:file-versions.file-id
                                        [(sql/max :file-versions.created-at) :created-at]}
                            :from     [:file-versions]
                            :group-by [:file-versions.file-id]}
                           :version]
                          [:= #{:version.file-id :files.id}]

                          [:file-versions :fv]
                          [:and
                           #{[:= #{:fv.file-id :version.file-id}]
                             [:= #{:fv.created-at :version.created-at}]}]]
                         (-> join
                             (update-in [0 0 :select] set)
                             (update 1 tu/op-set)
                             (update-in [3 1] tu/op-set)
                             (update-in [3 2] tu/op-set)
                             (update 3 tu/op-set))))))

              (testing "emits an event"
                (let [{event-id :event/id :as event} (-> ch
                                                         (stubs/calls :send!)
                                                         colls/only!
                                                         first)]
                  (is (uuid? event-id))
                  (is (= {:event/id         event-id
                          :event/type       :file/created
                          :event/model-id   file-id
                          :event/data       file
                          :event/emitted-by user-id
                          :event/ctx        {:user/id    user-id
                                             :project/id project-id}}
                         event))))))

          (testing "and when the executor throws an exception"
            (let [request-id (uuids/random)
                  user-id (uuids/random)]
              (stubs/init! ch)
              (stubs/use! tx :execute!
                          (ex-info "Executor" {}))
              (int/handle! handler
                           {:command/type :file/create!
                            :command/data {}
                            :command/ctx  {:user/id    user-id
                                           :request/id request-id}})

              (testing "does not emit a successful event"
                (empty? (stubs/calls ch :send!)))

              (testing "emits a command-failed event"
                (let [{event-id :event/id :as event} (-> ch
                                                         (stubs/calls :send!)
                                                         colls/only!
                                                         first)]
                  (is (uuid? event-id))
                  (is (= {:event/id         event-id
                          :event/model-id   request-id
                          :event/type       :command/failed
                          :event/data       {:error/command :file/create!
                                             :error/reason  "Executor"}
                          :event/emitted-by user-id
                          :event/ctx        {:request/id request-id `:user/id user-id}}
                         event))))))

          (testing "and when the pubsub throws an exception"
            (let [request-id (uuids/random)
                  user-id (uuids/random)]
              (stubs/init! ch)
              (stubs/use! tx :execute!
                          [{:id "file-id"}]
                          [{:id file-id}]
                          [file])
              (stubs/use! ch :send!
                          (ex-info "Channel" {}))
              (int/handle! handler
                           {:command/type :file/create!
                            :command/data {}
                            :command/ctx  {:user/id    user-id
                                           :request/id request-id}})

              (testing "emits a command-failed event"
                (let [{event-id :event/id :as event} (-> ch
                                                         (stubs/calls :send!)
                                                         rest
                                                         colls/only!
                                                         first)]
                  (is (uuid? event-id))
                  (is (= {:event/id         event-id
                          :event/model-id   request-id
                          :event/type       :command/failed
                          :event/data       {:error/command :file/create!
                                             :error/reason  "Channel"}
                          :event/emitted-by user-id
                          :event/ctx        {:request/id request-id
                                             :user/id    user-id}}
                         event))))))))

      (testing "when saving file-versions"
        (let [[artifact-id file-id project-id user-id version-id] (repeatedly uuids/random)
              version {:file-version/name "version name here"
                       :file-version/id   version-id}
              ch (ts/->chan)
              tx (trepos/stub-transactor db.files/->FilesRepoExecutor)
              handler (pub.files/->FileCommandHandler tx ch)]
          (testing "and when creating a version"
            (stubs/use! tx :execute!
                        [{:id project-id}]
                        [{:id version-id}]
                        [version]
                        [{:id "event-id"}])
            (int/handle! handler
                         {:command/type :file-version/create!
                          :command/data {:file/name    "file name"
                                         :version/name "version"
                                         :artifact/id  artifact-id}
                          :command/ctx  {:user/id user-id
                                         :file/id file-id}})
            (let [[[access-query] [version-insert] [query-for-event]] (colls/only! 3 (stubs/calls tx :execute!))]
              (testing "checks for access permission"
                (is (= [:projects] (:from access-query)))
                (is (= [:and
                        #{[:= #{:user-teams.user-id user-id}]
                          [:= #{:files.id file-id}]}]
                       (-> access-query
                           :where
                           (update 1 tu/op-set)
                           (update 2 tu/op-set)
                           tu/op-set)))
                (is (= [:user-teams
                        [:= #{:user-teams.team-id :projects.team-id}]
                        :files
                        [:= #{:files.project-id :projects.id}]]
                       (-> access-query
                           :join
                           (update 1 tu/op-set)
                           (update 3 tu/op-set)))))

              (testing "saves version data to the repository"
                (is (= {:insert-into :file-versions
                        :values      [{:artifact-id artifact-id
                                       :file-id     file-id
                                       :name        "version"}]
                        :returning   #{:id}}
                       (update version-insert :returning set))))

              (testing "queries a file from the repository"
                (let [{:keys [select from where]} query-for-event]
                  (is (= #{[:file-versions.id "file-version/id"]
                           [:file-versions.name "file-version/name"]
                           [:file-versions.artifact-id "file-version/artifact-id"]
                           [:file-versions.created-at "file-version/created-at"]
                           [:file-versions.file-id "file-version/file-id"]}
                         (set select)))
                  (is (= [:file-versions] from))
                  (is (= [:= #{:file-versions.id version-id}]
                         (tu/op-set where))))))

            (testing "emits an event"
              (let [{event-id :event/id :as event} (-> ch
                                                       (stubs/calls :send!)
                                                       colls/only!
                                                       first)]
                (is (uuid? event-id))
                (is (= {:event/id         event-id
                        :event/type       :file-version/created
                        :event/model-id   version-id
                        :event/data       version
                        :event/emitted-by user-id
                        :event/ctx        {:user/id user-id
                                           :file/id file-id}}
                       event)))))

          (testing "and when the executor throws an exception"
            (let [request-id (uuids/random)
                  user-id (uuids/random)]
              (stubs/init! ch)
              (stubs/use! tx :execute!
                          (ex-info "Executor" {}))
              (int/handle! handler
                           {:command/type :file-version/create!
                            :command/data {}
                            :command/ctx  {:user/id    user-id
                                           :request/id request-id}})

              (testing "does not emit a successful event"
                (empty? (stubs/calls ch :send!)))

              (testing "emits a command-failed event"
                (let [{event-id :event/id :as event} (-> ch
                                                         (stubs/calls :send!)
                                                         colls/only!
                                                         first)]
                  (is (uuid? event-id))
                  (is (= {:event/id         event-id
                          :event/model-id   request-id
                          :event/type       :command/failed
                          :event/data       {:error/command :file-version/create!
                                             :error/reason  "Executor"}
                          :event/emitted-by user-id
                          :event/ctx        {:request/id request-id
                                             :user/id    user-id}}
                         event))))))

          (testing "and when the pubsub throws an exception"
            (let [request-id (uuids/random)
                  user-id (uuids/random)]
              (stubs/init! ch)
              (stubs/use! tx :execute!
                          [{:id "file-version-id"}])
              (stubs/use! ch :send!
                          (ex-info "Channel" {}))
              (int/handle! handler
                           {:command/type :file-version/create!
                            :command/data {}
                            :command/ctx  {:user/id    user-id
                                           :request/id request-id}})

              (testing "emits a command-failed event"
                (let [{event-id :event/id :as event} (-> ch
                                                         (stubs/calls :send!)
                                                         rest
                                                         colls/only!
                                                         first)]
                  (is (uuid? event-id))
                  (is (= {:event/id         event-id
                          :event/model-id   request-id
                          :event/type       :command/failed
                          :event/data       {:error/command :file-version/create!
                                             :error/reason  "Channel"}
                          :event/emitted-by user-id
                          :event/ctx        {:request/id request-id
                                             :user/id    user-id}}
                         event)))))))))))
