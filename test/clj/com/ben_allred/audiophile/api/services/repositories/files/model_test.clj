(ns ^:unit com.ben-allred.audiophile.api.services.repositories.files.model-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.entities.sql :as sql]
    [com.ben-allred.audiophile.api.services.repositories.files.model :as files]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.uuids :as uuids]
    [test.utils :as tu]
    [test.utils.mocks :as mocks]
    [test.utils.repositories :as trepos]))

(deftest create-artifact-test
  (testing "create-artifact"
    (let [store (trepos/mock-kv-store)
          tx (trepos/mock-transactor store)]
      (testing "when the content saves to the kv-store"
        (let [[artifact-id user-id] (repeatedly uuids/random)]
          (mocks/set-mock! tx :execute! [{:id artifact-id}])
          (let [result (files/create-artifact tx
                                              {:filename     "file.name"
                                               :size         12345
                                               :content-type "content/type"
                                               :tempfile     "…content…"}
                                              user-id)
                [store-k & stored] (colls/only! (mocks/calls store :put!))
                [query] (colls/only! (mocks/calls tx :execute!))]
            (testing "sends the data to the kv store"
              (is (= ["…content…" {:content-type   "content/type"
                                   :content-length 12345
                                   :metadata       {:filename "file.name"}}]
                     stored)))

            (testing "sends a query to the repository"
              (is (= {:insert-into :artifacts
                      :values      [{:filename     "file.name"
                                     :content-type "content/type"
                                     :uri          (repos/uri store store-k)
                                     :created-by   user-id}]
                      :returning   [:id]}
                     query)))

            (testing "returns the result"
              (is (= {:artifact/id       artifact-id
                      :artifact/filename "file.name"}
                     result))))))

      (testing "when the store throws an exception"
        (mocks/use! store :put!
                    (ex-info "Store" {}))
        (testing "fails"
          (is (thrown? Throwable (files/create-artifact tx {} (uuids/random))))))

      (testing "when the store throws an exception"
        (mocks/use! tx :execute!
                    (ex-info "Executor" {}))
        (testing "fails"
          (is (thrown? Throwable (files/create-artifact tx {} (uuids/random)))))))))

(deftest query-many-test
  (testing "query-many"
    (let [[project-id user-id] (repeatedly uuids/random)
          tx (trepos/mock-transactor)]
      (testing "when querying files"
        (mocks/set-mock! tx :execute! [{:some :result}])
        (let [result (files/query-many tx project-id user-id)
              [query] (colls/only! (mocks/calls tx :execute!))]
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
              (is (= [:exists {:select [:id]
                               :from   [:projects]
                               :join   [:user-teams [:= #{:projects.team-id :user-teams.team-id}]]
                               :where  [:and
                                        #{[:= #{:projects.id project-id}]
                                          [:= #{:user-teams.user-id user-id}]}]}]
                     (-> where
                         (update-in [1 :join 1] tu/op-set)
                         (update-in [1 :where 1] tu/op-set)
                         (update-in [1 :where 2] tu/op-set)
                         (update-in [1 :where] tu/op-set))))
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
          tx (trepos/mock-transactor)]
      (testing "when creating a file"
        (mocks/use! tx :execute!
                    [{:id project-id}]
                    [{:id file-id}]
                    nil
                    [{:id file-id :other :data}])
        (let [result (files/create-file tx
                                        project-id
                                        {:file/name    "file name"
                                         :version/name "version"
                                         :artifact/id  artifact-id}
                                        user-id)
              [[access-query] [file-insert] [version-insert] [query] & more] (mocks/calls tx :execute!)]
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
              (let [[sql-fn [query fallback]] (-> value
                                                  :idx
                                                  ((juxt :name :args)))
                    {:keys [select from where join]} query]
                (is (= :coalesce sql-fn))
                (is (zero? fallback))
                (is (= #{(sql/max :idx)} (set select)))
                (is (= [:files] from))
                (is (= [:= #{:files.project-id project-id}]
                       (tu/op-set where)))
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
                           (update 3 tu/op-set)))))))

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
        (mocks/use! tx :execute!
                    (ex-info "Executor" {}))
        (testing "fails"
          (is (thrown? Throwable (files/create-file tx
                                                    (uuids/random)
                                                    {}
                                                    (uuids/random)))))))))

(deftest create-file-version-test
  (testing "create-file-version"
    (let [[artifact-id file-id project-id user-id] (repeatedly uuids/random)
          tx (trepos/mock-transactor)]
      (testing "when creating a version"
        (mocks/use! tx :execute!
                    [{:id project-id}]
                    nil
                    [{:id file-id :other :data}])
        (let [result (files/create-file-version tx
                                                file-id
                                                {:file/name    "file name"
                                                 :version/name "version"
                                                 :artifact/id  artifact-id}
                                                user-id)
              [[access-query] [version-insert] [query] & more] (mocks/calls tx :execute!)]
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
        (mocks/use! tx :execute!
                    (ex-info "Executor" {}))
        (testing "fails"
          (is (thrown? Throwable (files/create-file-version tx
                                                            (uuids/random)
                                                            {}
                                                            (uuids/random)))))))))