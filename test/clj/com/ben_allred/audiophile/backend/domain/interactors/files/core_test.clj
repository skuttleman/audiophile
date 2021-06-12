(ns ^:unit com.ben-allred.audiophile.backend.domain.interactors.files.core-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.files.core :as rfiles]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.backend.infrastructure.db.events :as db.events]
    [com.ben-allred.audiophile.backend.infrastructure.db.files :as db.files]
    [com.ben-allred.audiophile.backend.infrastructure.db.models.sql :as sql]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.ws :as ws]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [com.ben-allred.audiophile.common.infrastructure.pubsub.protocols :as ppubsub]
    [test.utils :as tu]
    [test.utils.repositories :as trepos]
    [test.utils.stubs :as stubs]))

(defn ^:private ->file-executor
  ([config]
   (fn [executor models]
     (->file-executor executor (merge models config))))
  ([executor {:keys [artifacts event-types events file-versions
                     files projects pubsub store user-teams]}]
   (let [file-exec (db.files/->FilesRepoExecutor executor
                                                 artifacts
                                                 file-versions
                                                 files
                                                 projects
                                                 user-teams
                                                 store
                                                 (constantly ::key))
         event-exec (db.events/->EventsExecutor executor event-types events)
         emitter (db.files/->FilesEventEmitter event-exec pubsub)]
     (db.files/->Executor file-exec emitter))))

(deftest create-artifact-test
  (testing "create-artifact"
    (let [store (trepos/stub-kv-store)
          pubsub (stubs/create (reify
                                 ppubsub/IPubSub
                                 (publish! [_ _ _])
                                 (subscribe! [_ _ _ _])
                                 (unsubscribe! [_ _])
                                 (unsubscribe! [_ _ _])))
          tx (trepos/stub-transactor (->file-executor {:pubsub pubsub
                                                       :store  store}))
          repo (rfiles/->FileAccessor tx)]
      (testing "when the content saves to the kv-store"
        (let [[artifact-id user-id] (repeatedly uuids/random)
              artifact {:artifact/id artifact-id
                        :artifact/uri "some-uri"
                        :artifact/filename "file.name"}]
          (stubs/use! tx :execute!
                      [{:id artifact-id}]
                      [artifact]
                      [{:id "event-id"}])
          (let [result (int/create-artifact! repo
                                             {:filename     "file.name"
                                              :size         12345
                                              :content-type "content/type"
                                              :tempfile     "…content…"
                                              :user/id      user-id})
                [store-k & stored] (colls/only! (stubs/calls store :put!))
                [[insert-artifact] [query-artifact] [insert-event]] (colls/only! 3 (stubs/calls tx :execute!))]
            (testing "sends the data to the kv store"
              (is (= ["…content…" {:content-type   "content/type"
                                   :content-length 12345
                                   :metadata       {:filename "file.name"}}]
                     stored)))

            (testing "inserts the artifact in the repository"
              (is (= {:insert-into :artifacts
                      :values      [{:filename     "file.name"
                                     :content-type "content/type"
                                     :uri          (repos/uri store store-k)
                                     :created-by   user-id}]
                      :returning   [:id]}
                     insert-artifact)))

            (testing "queries the event payload"
              (let [{:keys [select from where]} query-artifact]
                (is (= #{[:artifacts.filename "artifact/filename"]
                         [:artifacts.created-by "artifact/created-by"]
                         [:artifacts.id "artifact/id"]
                         [:artifacts.content-type "artifact/content-type"]
                         [:artifacts.uri "artifact/uri"]
                         [:artifacts.content-size "artifact/content-size"]
                         [:artifacts.created-at "artifact/created-at"]}
                       (set select)))
                (is (= [:artifacts] from))
                (is (= [:= #{:artifacts.id artifact-id}]
                       (tu/op-set where)))))

            (testing "inserts the event in the repository"
              (let [{:keys [event-type-id] :as value} (colls/only! (:values insert-event))]
                (is (= {:insert-into :events
                        :returning [:id]}
                       (dissoc insert-event :values)))
                (is (= {:model-id      artifact-id
                        :data          artifact
                        :emitted-by    user-id}
                       (dissoc value :event-type-id)))
                (is (= {:select #{[:event-types.id "event-type/id"]}
                        :from   [:event-types]
                        :where  [:and #{[:= #{:event-types.category "artifact"}]
                                        [:= #{:event-types.name "created"}]}]}
                       (-> event-type-id
                           (update :select set)
                           (update-in [:where 1] tu/op-set)
                           (update-in [:where 2] tu/op-set)
                           (update :where tu/op-set))))))

            (testing "emits an event"
              (let [[topic [event-id event]] (colls/only! (stubs/calls pubsub :publish!))]
                (is (= [::ws/user user-id] topic))
                (is (= "event-id" event-id))
                (is (= {:event/id "event-id"
                        :event/type :artifact/created
                        :event/model-id artifact-id
                        :event/data artifact
                        :event/emitted-by user-id}
                       event))))

            (testing "returns the result"
              (is (= artifact result))))))

      (testing "when the store throws an exception"
        (stubs/use! store :put!
                    (ex-info "Store" {}))
        (testing "fails"
          (is (thrown? Throwable (int/create-artifact! repo {:user/id (uuids/random)})))))

      (testing "when the store throws an exception"
        (stubs/use! tx :execute!
                    (ex-info "Executor" {}))
        (testing "fails"
          (is (thrown? Throwable (int/create-artifact! repo {:user/id (uuids/random)}))))))))

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

(deftest create-file-test
  (testing "create-file"
    (let [[artifact-id file-id project-id user-id] (repeatedly uuids/random)
          tx (trepos/stub-transactor ->file-executor)
          repo (rfiles/->FileAccessor tx)]
      (testing "when creating a file"
        (stubs/use! tx :execute!
                    [{:id project-id}]
                    [{:id file-id}]
                    nil
                    [{:id file-id :other :data}])
        (let [result (int/create-file! repo
                                       {:project/id   project-id
                                        :file/name    "file name"
                                        :version/name "version"
                                        :artifact/id  artifact-id
                                        :user/id      user-id})
              [[access-query] [file-insert] [version-insert] [query] & more] (stubs/calls tx :execute!)]
          (is (empty? more))
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
              (is (= user-id (:created-by value)))
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
                                   :name        "version"
                                   :created-by  user-id}]
                    :returning   #{:id}}
                   (update version-insert :returning set))))

          (testing "queries a file from the repository"
            (let [{:keys [select from where join]} query]
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

          (testing "returns the result"
            (is (= {:id    file-id
                    :other :data}
                   result)))))

      (testing "when the executor throws"
        (stubs/use! tx :execute!
                    (ex-info "Executor" {}))
        (testing "fails"
          (is (thrown? Throwable (int/create-file! repo
                                                   {:project/id (uuids/random)
                                                    :user/id    (uuids/random)}))))))))

(deftest create-file-version-test
  (testing "create-file-version"
    (let [[artifact-id file-id project-id user-id] (repeatedly uuids/random)
          tx (trepos/stub-transactor ->file-executor)
          repo (rfiles/->FileAccessor tx)]
      (testing "when creating a version"
        (stubs/use! tx :execute!
                    [{:id project-id}]
                    nil
                    [{:id file-id :other :data}])
        (let [result (int/create-file-version! repo
                                               {:file/id      file-id
                                                :file/name    "file name"
                                                :version/name "version"
                                                :artifact/id  artifact-id
                                                :user/id      user-id})
              [[access-query] [version-insert] [query] & more] (stubs/calls tx :execute!)]
          (is (empty? more))
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
                                   :name        "version"
                                   :created-by  user-id}]
                    :returning   #{:id}}
                   (update version-insert :returning set))))

          (testing "queries a file from the repository"
            (let [{:keys [select from where join]} query]
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

          (testing "returns the result"
            (is (= {:id    file-id
                    :other :data}
                   result)))))

      (testing "when the executor throws"
        (stubs/use! tx :execute!
                    (ex-info "Executor" {}))
        (testing "fails"
          (is (thrown? Throwable (int/create-file-version! repo
                                                           {:file/id (uuids/random)
                                                            :user/id (uuids/random)}))))))))
