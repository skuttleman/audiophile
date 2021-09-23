(ns ^:unit com.ben-allred.audiophile.backend.api.repositories.files.impl-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.files.impl :as rfiles]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.backend.infrastructure.db.files :as db.files]
    [com.ben-allred.audiophile.backend.infrastructure.db.models.sql :as sql]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.core :as ps]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.fns :as fns]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [com.ben-allred.audiophile.common.infrastructure.pubsub.protocols :as ppubsub]
    [test.utils :as tu]
    [test.utils.repositories :as trepos]
    [test.utils.stubs :as stubs]))

(defn ^:private ->file-executor
  ([config]
   (fn [executor models]
     (->file-executor executor (merge models config))))
  ([executor {:keys [artifacts file-versions files projects pubsub store user-teams]}]
   (let [file-exec (db.files/->FilesRepoExecutor executor
                                                 artifacts
                                                 file-versions
                                                 files
                                                 projects
                                                 user-teams
                                                 store
                                                 (constantly ::key))]
     (db.files/->Executor file-exec pubsub))))

(deftest create-artifact-test
  (testing "create-artifact"
    (let [store (trepos/stub-kv-store)
          pubsub (stubs/create (reify
                                 ppubsub/IPub
                                 (publish! [_ _ _])
                                 ppubsub/ISub
                                 (subscribe! [_ _ _ _])
                                 (unsubscribe! [_ _])
                                 (unsubscribe! [_ _ _])))
          tx (trepos/stub-transactor (->file-executor {:pubsub pubsub
                                                       :store  store}))
          repo (rfiles/->FileAccessor tx)]
      (testing "when the content saves to the kv-store"
        (let [[artifact-id user-id request-id] (repeatedly uuids/random)
              artifact {:artifact/id       artifact-id
                        :artifact/uri      "some-uri"
                        :artifact/filename "file.name"}]
          (stubs/use! tx :execute!
                      [{:id artifact-id}]
                      [artifact]
                      [{:id "event-id"}])
          @(int/create-artifact! repo
                                 {:filename     "file.name"
                                  :size         12345
                                  :content-type "content/type"
                                  :tempfile     "…content…"}
                                 {:user/id    user-id
                                  :request/id request-id})
          (let [[store-k & stored] (colls/only! (stubs/calls store :put!))
                [[insert-artifact] [query-artifact]] (colls/only! 2 (stubs/calls tx :execute!))]
            (testing "sends the data to the kv store"
              (is (= ["…content…" {:content-type   "content/type"
                                   :content-length 12345
                                   :metadata       {:filename "file.name"}}]
                     stored)))

            (testing "inserts the artifact in the repository"
              (is (= {:insert-into :artifacts
                      :values      [{:filename     "file.name"
                                     :content-type "content/type"
                                     :uri          (repos/uri store store-k)}]
                      :returning   [:id]}
                     insert-artifact)))

            (testing "queries the event payload"
              (let [{:keys [select from where]} query-artifact]
                (is (= #{[:artifacts.filename "artifact/filename"]
                         [:artifacts.id "artifact/id"]
                         [:artifacts.content-type "artifact/content-type"]
                         [:artifacts.uri "artifact/uri"]
                         [:artifacts.content-size "artifact/content-size"]
                         [:artifacts.created-at "artifact/created-at"]}
                       (set select)))
                (is (= [:artifacts] from))
                (is (= [:= #{:artifacts.id artifact-id}]
                       (tu/op-set where)))))

            (testing "emits an event"
              (let [[topic [event-id event]] (colls/only! (stubs/calls pubsub :publish!))]
                (is (= [::ps/user user-id] topic))
                (is (uuid? event-id))
                (is (= {:event/id         event-id
                        :event/type       :artifact/created
                        :event/model-id   artifact-id
                        :event/data       artifact
                        :event/emitted-by user-id}
                       event)))))))

      (testing "when the store throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/use! store :put!
                      (ex-info "Store" {}))
          @(int/create-artifact! repo {} {:user/id user-id :request/id request-id})
          (testing "does not emit a successful event"
            (empty? (stubs/calls pubsub :publish!)))

          (testing "emits a command-failed event"
            (let [[topic [event-id event ctx]] (-> pubsub
                                                   (stubs/calls :publish!)
                                                   rest
                                                   colls/only!)]
              (is (= [::ps/user user-id] topic))
              (is (uuid? event-id))
              (is (= {:event/id         event-id
                      :event/model-id   request-id
                      :event/type       :command/failed
                      :event/data       {:error/command :artifact/create!
                                         :error/reason  "insufficient access to create artifact"}
                      :event/emitted-by user-id}
                     event))
              (is (= {:request/id request-id
                      :user/id    user-id}
                     ctx))))))

      (testing "when the executor throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! pubsub)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          @(int/create-artifact! repo {} {:user/id user-id :request/id request-id})
          (testing "does not emit a successful event"
            (empty? (stubs/calls pubsub :publish!)))

          (testing "emits a command-failed event"
            (let [[topic [event-id event ctx]] (-> pubsub
                                                   (stubs/calls :publish!)
                                                   colls/only!)]
              (is (= [::ps/user user-id] topic))
              (is (uuid? event-id))
              (is (= {:event/id         event-id
                      :event/model-id   request-id
                      :event/type       :command/failed
                      :event/data       {:error/command :artifact/create!
                                         :error/reason  "insufficient access to create artifact"}
                      :event/emitted-by user-id}
                     event))
              (is (= {:request/id request-id
                      :user/id    user-id}
                     ctx))))))

      (testing "when the pubsub throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! pubsub)
          (stubs/use! pubsub :publish!
                      (ex-info "Executor" {}))
          @(int/create-artifact! repo {} {:user/id user-id :request/id request-id})
          (testing "emits a command-failed event"
            (let [[topic [event-id event ctx]] (-> pubsub
                                                   (stubs/calls :publish!)
                                                   rest
                                                   colls/only!)]
              (is (= [::ps/user user-id] topic))
              (is (uuid? event-id))
              (is (= {:event/id         event-id
                      :event/model-id   request-id
                      :event/type       :command/failed
                      :event/data       {:error/command :artifact/create!
                                         :error/reason  "insufficient access to create artifact"}
                      :event/emitted-by user-id}
                     event))
              (is (= {:request/id request-id
                      :user/id    user-id}
                     ctx)))))))))

(deftest query-many-test
  (testing "query-many"
    (let [[project-id user-id] (repeatedly uuids/random)
          tx (trepos/stub-transactor ->file-executor)
          repo (rfiles/->FileAccessor tx)]
      (testing "when querying files"
        (stubs/set-stub! tx :execute! [{:some :result}])
        (let [result (int/query-many repo {:user/id    user-id
                                           :project/id project-id})
              [query] (colls/only! (stubs/calls tx :execute!))]
          (testing "sends a query to the repository"
            (let [{:keys [select from where join order-by]} query]
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
              (let [[clause & clauses] where
                    clauses' (into {} (map (juxt tu/op-set identity)) clauses)]
                (is (= :and clause))
                (is (contains? clauses' [:= #{:files.project-id project-id}]))
                (is (= [:exists {:select [1]
                                 :from   [:projects]
                                 :join   [:user-teams [:= #{:projects.team-id :user-teams.team-id}]]
                                 :where  [:and
                                          #{[:= #{:projects.id :files.project-id}]
                                            [:= #{:user-teams.user-id user-id}]}]}]
                       (-> clauses'
                           (dissoc [:= #{:files.project-id project-id}])
                           colls/only!
                           val
                           (update-in [1 :join 1] tu/op-set)
                           (update-in [1 :where 1] tu/op-set)
                           (update-in [1 :where 2] tu/op-set)
                           (update-in [1 :where] tu/op-set)))))
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
                         (update 3 tu/op-set))))
              (is (= [[:files.idx :asc]
                      [:version.created-at :desc]]
                     order-by))))

          (testing "returns the result"
            (is (= [{:some :result}] result))))))))

(deftest query-one-test
  (testing "query-one"
    (let [[file-id user-id] (repeatedly uuids/random)
          tx (trepos/stub-transactor ->file-executor)
          repo (rfiles/->FileAccessor tx)]
      (testing "when querying one file"
        (stubs/set-stub! tx :execute! [{:some :result}])
        (let [result (int/query-one repo {:user/id user-id
                                          :file/id file-id})
              [[file-query] [version-query]] (colls/only! 2 (stubs/calls tx :execute!))]
          (testing "sends a query to the repository"
            (let [{:keys [select from where]} file-query]
              (is (= #{[:files.id "file/id"]
                       [:files.name "file/name"]
                       [:files.project-id "file/project-id"]
                       [:files.idx "file/idx"]}
                     (set select)))
              (is (= [:files] from))
              (is (= :and (first where)))
              (is (= [:= #{:files.id file-id}]
                     (tu/op-set (second where))))
              (is (= [:exists {:select #{1}
                               :from   [:projects]
                               :where  [:and
                                        #{[:= #{:user-teams.user-id user-id}]
                                          [:= #{:projects.id :files.project-id}]}]
                               :join   [:user-teams [:= #{:user-teams.team-id :projects.team-id}]]}]
                     (-> where
                         (get 2)
                         (update 1 (fns/=>
                                     (update :select set)
                                     (update :where (fns/=> (update 1 tu/op-set)
                                                            (update 2 tu/op-set)
                                                            tu/op-set))
                                     (update-in [:join 1] tu/op-set))))))))

          (testing "returns the result"
            (is (= {:some :result :file/versions [{:some :result}]}
                   result)))))

      (testing "when the file is not found for the user"
        (stubs/init! tx)
        (stubs/set-stub! tx :execute! [])
        (let [result (int/query-one repo {:user/id user-id
                                          :file/id file-id})]
          (is (= 1 (count (stubs/calls tx :execute!))))
          (is (nil? result)))))))

(deftest query-artifact-test
  (testing "query-artifact"
    (let [[artifact-id user-id] (repeatedly uuids/random)
          store (trepos/stub-kv-store)
          tx (trepos/stub-transactor (->file-executor {:store store}))
          repo (rfiles/->FileAccessor tx)]
      (testing "when querying an artifact"
        (stubs/set-stub! tx :execute! [{:some                  :result
                                        :artifact/key          "some-key"
                                        :artifact/content-type "content-type"}])
        (stubs/set-stub! store :get ::artifact-blob)
        (let [result (int/get-artifact repo {:artifact/id artifact-id
                                             :user/id     user-id})
              [query] (colls/only! (stubs/calls tx :execute!))
              [lookup] (colls/only! (stubs/calls store :get))]
          (testing "sends a query to the repository"
            (let [{:keys [select from where]} query]
              (is (= #{[:artifacts.filename "artifact/filename"]
                       [:artifacts.id "artifact/id"]
                       [:artifacts.content-type "artifact/content-type"]
                       [:artifacts.uri "artifact/uri"]
                       [:artifacts.content-size "artifact/content-size"]
                       [:artifacts.created-at "artifact/created-at"]}
                     (set select)))
              (is (= [:artifacts] from))
              (is (= :and (first where)))
              (is (= [:= #{:artifacts.id artifact-id}]
                     (tu/op-set (second where))))
              (is (= [:exists {:select [1]
                               :from   [:projects]
                               :where  [:and #{[:= #{:user-teams.user-id user-id}]
                                               [:= #{:file-versions.artifact-id :artifacts.id}]}]
                               :join   [:user-teams [:= #{:user-teams.team-id :projects.team-id}]
                                        :files [:= #{:files.project-id :projects.id}]
                                        :file-versions [:= #{:file-versions.file-id :files.id}]]}]
                     (-> where
                         rest
                         second
                         (update-in [1 :where] (fns/=> (update 1 tu/op-set)
                                                       (update 2 tu/op-set)
                                                       tu/op-set))
                         (update-in [1 :join] (fns/=> (update 1 tu/op-set)
                                                      (update 3 tu/op-set)
                                                      (update 5 tu/op-set))))))))

          (testing "looks up the artifact in the kv store"
            (is (= "some-key" lookup)))

          (testing "returns the result"
            (is (= [::artifact-blob {:content-type "content-type"}]
                   result)))))

      (testing "when the artifact is not found for the user"
        (stubs/init! tx)
        (stubs/init! store)
        (stubs/set-stub! tx :execute! [])
        (let [result (int/query-one repo {:user/id     user-id
                                          :artifact/id artifact-id})]
          (is (= 1 (count (stubs/calls tx :execute!))))
          (is (empty? (stubs/calls store :get)))
          (is (nil? result)))))))

(deftest create-file-test
  (testing "create-file"
    (let [[artifact-id file-id project-id user-id] (repeatedly uuids/random)
          file {:file/id         file-id
                :file/name       "file name"
                :file/project-id project-id}
          pubsub (stubs/create (reify
                                 ppubsub/IPub
                                 (publish! [_ _ _])
                                 ppubsub/ISub
                                 (subscribe! [_ _ _ _])
                                 (unsubscribe! [_ _])
                                 (unsubscribe! [_ _ _])))
          tx (trepos/stub-transactor (->file-executor {:pubsub pubsub}))
          repo (rfiles/->FileAccessor tx)]
      (testing "when creating a file"
        (stubs/use! tx :execute!
                    [{:id project-id}]
                    [{:id file-id}]
                    nil
                    [file]
                    [{:id "event-id"}])
        @(int/create-file! repo
                           {:file/name    "file name"
                            :version/name "version"
                            :artifact/id  artifact-id}
                           {:user/id    user-id
                            :project/id project-id})
        (let [[[access-query] [file-insert] [version-insert] [query-for-event]] (colls/only! 4 (stubs/calls tx :execute!))]
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
            (let [[topic [event-id event]] (colls/only! (stubs/calls pubsub :publish!))]
              (is (= [::ps/user user-id] topic))
              (is (uuid? event-id))
              (is (= {:event/id         event-id
                      :event/type       :file/created
                      :event/model-id   file-id
                      :event/data       file
                      :event/emitted-by user-id}
                     event))))))

      (testing "when the executor throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! pubsub)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          @(int/create-file! repo {} {:user/id user-id :request/id request-id})
          (testing "does not emit a successful event"
            (empty? (stubs/calls pubsub :publish!)))

          (testing "emits a command-failed event"
            (let [[topic [event-id event ctx]] (-> pubsub
                                                   (stubs/calls :publish!)
                                                   colls/only!)]
              (is (= [::ps/user user-id] topic))
              (is (uuid? event-id))
              (is (= {:event/id         event-id
                      :event/model-id   request-id
                      :event/type       :command/failed
                      :event/data       {:error/command :file/create!
                                         :error/reason  "insufficient access to create file"}
                      :event/emitted-by user-id}
                     event))
              (is (= {:request/id request-id
                      :user/id    user-id}
                     ctx))))))

      (testing "when the pubsub throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! pubsub)
          (stubs/use! tx :execute!
                      [{:id "file-id"}]
                      [{:id file-id}]
                      [file])
          (stubs/use! pubsub :publish!
                      (ex-info "Executor" {}))
          @(int/create-file! repo {} {:user/id user-id :request/id request-id})
          (testing "emits a command-failed event"
            (let [[topic [event-id event ctx]] (-> pubsub
                                                   (stubs/calls :publish!)
                                                   rest
                                                   colls/only!)]
              (is (= [::ps/user user-id] topic))
              (is (uuid? event-id))
              (is (= {:event/id         event-id
                      :event/model-id   request-id
                      :event/type       :command/failed
                      :event/data       {:error/command :file/create!
                                         :error/reason  "insufficient access to create file"}
                      :event/emitted-by user-id}
                     event))
              (is (= {:request/id request-id
                      :user/id    user-id}
                     ctx)))))))))

(deftest create-file-version-test
  (testing "create-file-version"
    (let [[artifact-id file-id project-id user-id version-id] (repeatedly uuids/random)
          version {:file-version/name "version name here"
                   :file-version/id   version-id}
          pubsub (stubs/create (reify
                                 ppubsub/IPub
                                 (publish! [_ _ _])
                                 ppubsub/ISub
                                 (subscribe! [_ _ _ _])
                                 (unsubscribe! [_ _])
                                 (unsubscribe! [_ _ _])))
          tx (trepos/stub-transactor (->file-executor {:pubsub pubsub}))
          repo (rfiles/->FileAccessor tx)]
      (testing "when creating a version"
        (stubs/use! tx :execute!
                    [{:id project-id}]
                    [{:id version-id}]
                    [version]
                    [{:id "event-id"}])
        @(int/create-file-version! repo
                                   {:file/name    "file name"
                                    :version/name "version"
                                    :artifact/id  artifact-id}
                                   {:file/id file-id
                                    :user/id user-id})
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
          (let [[topic [event-id event]] (colls/only! (stubs/calls pubsub :publish!))]
            (is (= [::ps/user user-id] topic))
            (is (uuid? event-id))
            (is (= {:event/id         event-id
                    :event/type       :file-version/created
                    :event/model-id   version-id
                    :event/data       version
                    :event/emitted-by user-id}
                   event)))))

      (testing "when the executor throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! pubsub)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          @(int/create-file-version! repo {} {:user/id user-id :request/id request-id})
          (testing "does not emit a successful event"
            (empty? (stubs/calls pubsub :publish!)))

          (testing "emits a command-failed event"
            (let [[topic [event-id event ctx]] (-> pubsub
                                                   (stubs/calls :publish!)
                                                   colls/only!)]
              (is (= [::ps/user user-id] topic))
              (is (uuid? event-id))
              (is (= {:event/id         event-id
                      :event/model-id   request-id
                      :event/type       :command/failed
                      :event/data       {:error/command :file-version/create!
                                         :error/reason  "insufficient access to create file-version"}
                      :event/emitted-by user-id}
                     event))
              (is (= {:request/id request-id
                      :user/id    user-id}
                     ctx))))))

      (testing "when the pubsub throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! pubsub)
          (stubs/use! tx :execute!
                      [{:id "file-version-id"}])
          (stubs/use! pubsub :publish!
                      (ex-info "Executor" {}))
          @(int/create-file-version! repo {} {:user/id user-id :request/id request-id})
          (testing "emits a command-failed event"
            (let [[topic [event-id event ctx]] (-> pubsub
                                                   (stubs/calls :publish!)
                                                   rest
                                                   colls/only!)]
              (is (= [::ps/user user-id] topic))
              (is (uuid? event-id))
              (is (= {:event/id         event-id
                      :event/model-id   request-id
                      :event/type       :command/failed
                      :event/data       {:error/command :file-version/create!
                                         :error/reason  "insufficient access to create file-version"}
                      :event/emitted-by user-id}
                     event))
              (is (= {:request/id request-id
                      :user/id    user-id}
                     ctx)))))))))
