(ns ^:unit com.ben-allred.audiophile.backend.api.repositories.files.impl-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [com.ben-allred.audiophile.backend.api.repositories.files.impl :as rfiles]
    [com.ben-allred.audiophile.backend.domain.interactors.core :as int]
    [com.ben-allred.audiophile.backend.infrastructure.db.models.sql :as sql]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.fns :as fns]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [com.ben-allred.audiophile.test.utils :as tu]
    [com.ben-allred.audiophile.test.utils.repositories :as trepos]
    [com.ben-allred.audiophile.test.utils.stubs :as stubs]))

#_(deftest create-artifact-test
  (testing "create-artifact"
    ;; TODO - write test
    ))

(deftest query-many-test
  (testing "query-many"
    (let [[project-id user-id] (repeatedly uuids/random)
          tx (trepos/stub-transactor trepos/->file-executor)
          repo (rfiles/->FileAccessor tx nil nil nil)]
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
          tx (trepos/stub-transactor trepos/->file-executor)
          repo (rfiles/->FileAccessor tx nil nil nil)]
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
          tx (trepos/stub-transactor trepos/->file-executor)
          repo (rfiles/->FileAccessor tx store nil nil)]
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

#_(deftest create-file-test
  (testing "create-file"
    ;; TODO - write test
    ))

#_(deftest create-file-version-test
  (testing "create-file-version"
    ;; TODO - write test
    ))
