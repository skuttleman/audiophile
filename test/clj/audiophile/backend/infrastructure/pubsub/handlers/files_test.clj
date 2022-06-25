(ns ^:unit audiophile.backend.infrastructure.pubsub.handlers.files-test
  (:require
    [audiophile.backend.infrastructure.db.models.sql :as sql]
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils :as tu]
    [audiophile.test.utils.repositories :as trepos]
    [audiophile.test.utils.services :as ts]
    [audiophile.test.utils.stubs :as stubs]
    [clojure.test :refer [are deftest is testing]]
    audiophile.backend.infrastructure.pubsub.handlers.files))

(deftest artifact-create!-test
  (testing "wf/command-handler :file/create!"
    (let [ch (ts/->chan)
          tx (trepos/stub-transactor)]
      (testing "when the content saves to the kv-store"
        (let [[artifact-id user-id request-id spigot-id] (repeatedly uuids/random)
              artifact {:artifact/id       artifact-id
                        :artifact/uri      "some-uri"
                        :artifact/filename "file.name"}]
          (stubs/use! tx :execute!
                      [{:id artifact-id}]
                      [artifact]
                      [{:id "event-id"}])
          (repos/transact! tx wf/command-handler
                           {:commands ch}
                           {:command/type :artifact/create!
                            :command/data {:spigot/id     spigot-id
                                           :spigot/params {:artifact/filename     "file.name"
                                                           :artifact/size         12345
                                                           :artifact/content-type "content/type"
                                                           :artifact/uri          "some://uri"}}
                            :command/ctx  {:user/id    user-id
                                           :request/id request-id}})

          (let [[[insert-artifact] [query-artifact]] (colls/only! 2 (stubs/calls tx :execute!))]
            (testing "inserts the artifact in the repository"
              (is (= {:insert-into :artifacts
                      :values      [{:filename       "file.name"
                                     :content-type   "content/type"
                                     :uri            "some://uri"
                                     :content-length 12345}]
                      :returning   [:id]}
                     insert-artifact)))

            (testing "emits an command"
              (let [{command-id :command/id :as command} (-> ch
                                                             (stubs/calls :send!)
                                                             colls/only!
                                                             first)]
                (is (uuid? command-id))
                (is (= {:command/id         command-id
                        :command/type       :workflow/next!
                        :command/data       {:spigot/id     spigot-id
                                             :spigot/result {:artifact/id artifact-id}}
                        :command/emitted-by user-id
                        :command/ctx        {:user/id    user-id
                                             :request/id request-id}}
                       command)))))))

      (testing "when the executor throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! ch)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          (repos/transact! tx wf/command-handler
                           {:events ch}
                           {:command/type :artifact/create!
                            :command/data {:spigot/params {}}
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

      (testing "when the pubsub throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! ch)
          (stubs/use! ch :send!
                      (ex-info "Channel" {}))
          (repos/transact! tx wf/command-handler
                           {:commands ch :events ch}
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
                     event)))))))))

(deftest file-create!-test
  (testing "wf/command-handler :file/create!"
    (let [[artifact-id file-id project-id user-id spigot-id] (repeatedly uuids/random)
          file {:file/id         file-id
                :file/name       "file name"
                :file/project-id project-id}
          ch (ts/->chan)
          tx (trepos/stub-transactor)]
      (testing "and when creating a file"
        (stubs/use! tx :execute!
                    [{:id project-id}]
                    [{:id file-id}]
                    nil
                    [file]
                    [{:id "event-id"}])
        (repos/transact! tx wf/command-handler
                         {:commands ch}
                         {:command/type :file/create!
                          :command/data {:spigot/id     spigot-id
                                         :spigot/params {:artifact/id  artifact-id
                                                         :project/id   project-id
                                                         :file/name    "file name"
                                                         :version/name "version"}}
                          :command/ctx  {:user/id user-id}})

        (let [[[access-query] [file-insert]] (colls/only! 2 (stubs/calls tx :execute!))]
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

          (testing "emits an command"
            (let [{command-id :command/id :as command} (-> ch
                                                           (stubs/calls :send!)
                                                           colls/only!
                                                           first)]
              (is (uuid? command-id))
              (is (= {:command/id         command-id
                      :command/type       :workflow/next!
                      :command/data       {:spigot/id     spigot-id
                                           :spigot/result {:file/id file-id}}
                      :command/emitted-by user-id
                      :command/ctx        {:user/id user-id}}
                     command))))))

      (testing "and when the executor throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! ch)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          (repos/transact! tx wf/command-handler
                           {:events ch}
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
          (repos/transact! tx wf/command-handler
                           {:commands ch :events ch}
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
                     event)))))))))

(deftest file-version-create!-test
  (testing "wf/command-handler :file-version/create!"
    (let [[artifact-id file-id project-id user-id version-id spigot-id] (repeatedly uuids/random)
          version {:file-version/name "version name here"
                   :file-version/id   version-id}
          ch (ts/->chan)
          tx (trepos/stub-transactor)]
      (testing "and when creating a version"
        (stubs/use! tx :execute!
                    [{:id project-id}]
                    [{:id version-id}]
                    [version]
                    [{:id "event-id"}])
        (repos/transact! tx wf/command-handler
                         {:commands ch}
                         {:command/type :file-version/create!
                          :command/data {:spigot/id     spigot-id
                                         :spigot/params {:file/id      file-id
                                                         :file/name    "file name"
                                                         :version/name "version"
                                                         :artifact/id  artifact-id}}
                          :command/ctx  {:user/id user-id}})
        (let [[[access-query] [version-insert]] (colls/only! 2 (stubs/calls tx :execute!))]
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
                   (update version-insert :returning set)))))

        (testing "emits an command"
          (let [{command-id :command/id :as command} (-> ch
                                                         (stubs/calls :send!)
                                                         colls/only!
                                                         first)]
            (is (uuid? command-id))
            (is (= {:command/id         command-id
                    :command/type       :workflow/next!
                    :command/data       {:spigot/id     spigot-id
                                         :spigot/result {:file-version/id version-id}}
                    :command/emitted-by user-id
                    :command/ctx        {:user/id user-id}}
                   command)))))

      (testing "and when the executor throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! ch)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          (repos/transact! tx wf/command-handler
                           {:events ch}
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
          (repos/transact! tx wf/command-handler
                           {:commands ch :events ch}
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
                     event)))))))))
