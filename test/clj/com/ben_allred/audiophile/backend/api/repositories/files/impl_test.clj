(ns ^:unit com.ben-allred.audiophile.backend.api.repositories.files.impl-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.files.impl :as rfiles]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.infrastructure.db.events :as db.events]
    [com.ben-allred.audiophile.backend.infrastructure.db.files :as db.files]
    [com.ben-allred.audiophile.backend.infrastructure.db.models.sql :as sql]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.ws :as ws]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
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
  ([executor {:keys [artifacts emitter event-types events file-versions
                     files projects pubsub store user-events user-teams] :as models}]
   (let [file-exec (db.files/->FilesRepoExecutor executor
                                                 artifacts
                                                 file-versions
                                                 files
                                                 projects
                                                 user-teams
                                                 store
                                                 (constantly ::key))
         event-exec (db.events/->EventsExecutor executor
                                                event-types
                                                events
                                                user-events
                                                (db.events/->conform-fn models))
         emitter* (db.files/->FilesEventEmitter event-exec emitter pubsub)]
     (db.files/->Executor file-exec emitter*))))

(deftest create-artifact-test
  (testing "create-artifact"
    (let [store (trepos/stub-kv-store)
          pubsub (stubs/create (reify
                                 ppubsub/IPubSub
                                 (publish! [_ _ _])
                                 (subscribe! [_ _ _ _])
                                 (unsubscribe! [_ _])
                                 (unsubscribe! [_ _ _])))
          emitter (stubs/create (reify
                                  pint/IEmitter
                                  (command-failed! [_ _ _])))
          tx (trepos/stub-transactor (->file-executor {:emitter emitter
                                                       :pubsub  pubsub
                                                       :store   store}))
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
          @(int/create! repo
                        {:filename     "file.name"
                         :size         12345
                         :content-type "content/type"
                         :tempfile     "…content…"}
                        {:user/id    user-id
                         :request/id request-id})
          (let [[store-k & stored] (colls/only! (stubs/calls store :put!))
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

            (testing "inserts the event in the repository"
              (let [{:keys [event-type-id] :as value} (colls/only! (:values insert-event))]
                (is (= {:insert-into :events
                        :returning   [:id]}
                       (dissoc insert-event :values)))
                (is (= {:model-id   artifact-id
                        :data       artifact
                        :emitted-by user-id}
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
                (is (= {:event/id         "event-id"
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
          @(int/create! repo {} {:user/id user-id :request/id request-id})
          (testing "does not emit a successful event"
            (empty? (stubs/calls pubsub :publish!)))

          (testing "emits a command-failed event"
            (is (= [request-id {:user/id user-id :request/id request-id}]
                   (-> emitter
                       (stubs/calls :command-failed!)
                       colls/only!
                       (update 1 dissoc :error/reason)))))))

      (testing "when the executor throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! emitter)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          @(int/create! repo {} {:user/id user-id :request/id request-id})
          (testing "does not emit a successful event"
            (empty? (stubs/calls pubsub :publish!)))

          (testing "emits a command-failed event"
            (is (= [request-id {:user/id user-id :request/id request-id}]
                   (-> emitter
                       (stubs/calls :command-failed!)
                       colls/only!
                       (update 1 dissoc :error/reason)))))))

      (testing "when the pubsub throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! emitter)
          (stubs/use! pubsub :publish!
                      (ex-info "Executor" {}))
          @(int/create! repo {} {:user/id user-id :request/id request-id})
          (testing "emits a command-failed event"
            (is (= [request-id {:user/id user-id :request/id request-id}]
                   (-> emitter
                       (stubs/calls :command-failed!)
                       colls/only!
                       (update 1 dissoc :error/reason))))))))))

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
          file {:file/id         file-id
                :file/name       "file name"
                :file/project-id project-id}
          pubsub (stubs/create (reify
                                 ppubsub/IPubSub
                                 (publish! [_ _ _])
                                 (subscribe! [_ _ _ _])
                                 (unsubscribe! [_ _])
                                 (unsubscribe! [_ _ _])))
          emitter (stubs/create (reify
                                  pint/IEmitter
                                  (command-failed! [_ _ _])))
          tx (trepos/stub-transactor (->file-executor {:emitter emitter
                                                       :pubsub  pubsub}))
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
        (let [[[access-query] [file-insert] [version-insert] [query-for-event] [insert-event]] (colls/only! 5 (stubs/calls tx :execute!))]
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

          (testing "inserts the event in the repository"
            (let [{:keys [event-type-id] :as value} (colls/only! (:values insert-event))]
              (is (= {:insert-into :events
                      :returning   [:id]}
                     (dissoc insert-event :values)))
              (is (= {:model-id   file-id
                      :data       file
                      :emitted-by user-id}
                     (dissoc value :event-type-id)))
              (is (= {:select #{[:event-types.id "event-type/id"]}
                      :from   [:event-types]
                      :where  [:and #{[:= #{:event-types.category "file"}]
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
              (is (= {:event/id         "event-id"
                      :event/type       :file/created
                      :event/model-id   file-id
                      :event/data       file
                      :event/emitted-by user-id}
                     event))))))

      (testing "when the executor throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! emitter)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          @(int/create-file! repo {} {:user/id user-id :request/id request-id})
          (testing "does not emit a successful event"
            (empty? (stubs/calls pubsub :publish!)))

          (testing "emits a command-failed event"
            (is (= [request-id {:user/id user-id :request/id request-id}]
                   (-> emitter
                       (stubs/calls :command-failed!)
                       colls/only!
                       (update 1 dissoc :error/reason)))))))

      (testing "when the pubsub throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! emitter)
          (stubs/use! pubsub :publish!
                      (ex-info "Executor" {}))
          @(int/create-file! repo {} {:user/id user-id :request/id request-id})
          (testing "emits a command-failed event"
            (is (= [request-id {:user/id user-id :request/id request-id}]
                   (-> emitter
                       (stubs/calls :command-failed!)
                       colls/only!
                       (update 1 dissoc :error/reason))))))))))

(deftest create-file-version-test
  (testing "create-file-version"
    (let [[artifact-id file-id project-id user-id version-id] (repeatedly uuids/random)
          version {:file-version/name "version name here"
                   :file-version/id   version-id}
          pubsub (stubs/create (reify
                                 ppubsub/IPubSub
                                 (publish! [_ _ _])
                                 (subscribe! [_ _ _ _])
                                 (unsubscribe! [_ _])
                                 (unsubscribe! [_ _ _])))
          emitter (stubs/create (reify
                                  pint/IEmitter
                                  (command-failed! [_ _ _])))
          tx (trepos/stub-transactor (->file-executor {:emitter emitter
                                                       :pubsub  pubsub}))
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
        (let [[[access-query] [version-insert] [query-for-event] [insert-event]] (colls/only! 4 (stubs/calls tx :execute!))]
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
                     (tu/op-set where)))))

          (testing "inserts the event in the repository"
            (let [{:keys [event-type-id] :as value} (colls/only! (:values insert-event))]
              (is (= {:insert-into :events
                      :returning   [:id]}
                     (dissoc insert-event :values)))
              (is (= {:model-id   version-id
                      :data       version
                      :emitted-by user-id}
                     (dissoc value :event-type-id)))
              (is (= {:select #{[:event-types.id "event-type/id"]}
                      :from   [:event-types]
                      :where  [:and #{[:= #{:event-types.category "file-version"}]
                                      [:= #{:event-types.name "created"}]}]}
                     (-> event-type-id
                         (update :select set)
                         (update-in [:where 1] tu/op-set)
                         (update-in [:where 2] tu/op-set)
                         (update :where tu/op-set)))))))

        (testing "emits an event"
          (let [[topic [event-id event]] (colls/only! (stubs/calls pubsub :publish!))]
            (is (= [::ws/user user-id] topic))
            (is (= "event-id" event-id))
            (is (= {:event/id         "event-id"
                    :event/type       :file-version/created
                    :event/model-id   version-id
                    :event/data       version
                    :event/emitted-by user-id}
                   event)))))

      (testing "when the executor throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! emitter)
          (stubs/use! tx :execute!
                      (ex-info "Executor" {}))
          @(int/create-file-version! repo {} {:user/id user-id :request/id request-id})
          (testing "does not emit a successful event"
            (empty? (stubs/calls pubsub :publish!)))

          (testing "emits a command-failed event"
            (is (= [request-id {:user/id user-id :request/id request-id}]
                   (-> emitter
                       (stubs/calls :command-failed!)
                       colls/only!
                       (update 1 dissoc :error/reason)))))))

      (testing "when the pubsub throws an exception"
        (let [request-id (uuids/random)
              user-id (uuids/random)]
          (stubs/init! emitter)
          (stubs/use! pubsub :publish!
                      (ex-info "Executor" {}))
          @(int/create-file-version! repo {} {:user/id user-id :request/id request-id})
          (testing "emits a command-failed event"
            (is (= [request-id {:user/id user-id :request/id request-id}]
                   (-> emitter
                       (stubs/calls :command-failed!)
                       colls/only!
                       (update 1 dissoc :error/reason))))))))))