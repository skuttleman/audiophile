(ns ^:unit audiophile.backend.infrastructure.repositories.files.impl-test
  (:require
    [audiophile.backend.domain.interactors.core :as int]
    [audiophile.backend.infrastructure.repositories.files.impl :as rfiles]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.fns :as fns]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils :as tu]
    [audiophile.test.utils.assertions :as assert]
    [audiophile.test.utils.services :as ts]
    [audiophile.test.utils.stubs :as stubs]
    [clojure.test :refer [are deftest is testing]]
    [spigot.controllers.kafka.topologies :as sp.ktop]))

(deftest create-artifact-test
  (testing "create-artifact"
    (let [producer (ts/->chan)
          tx (ts/->tx)
          store (ts/->store)
          repo (rfiles/->FileAccessor tx store producer nil (constantly "key"))
          [request-id user-id] (repeatedly uuids/random)]
      (stubs/use! tx :execute! [{}])
      (stubs/set-stub! store :uri "some://uri")
      (int/create-artifact! repo
                            {:some :data}
                            {:some       :opts
                             :some/other :opts
                             :user/id    user-id
                             :request/id request-id})
      (testing "saves to the store"
        (is (= ["key" {:some :data :artifact/uri "some://uri" :artifact/key "key"}]
               (take 2 (colls/only! (stubs/calls store :put!))))))

      (testing "emits a command"
        (let [[{[tag params ctx] :value}] (colls/only! (stubs/calls producer :send!))]
          (is (= ::sp.ktop/create! tag))
          (assert/is? {:workflows/ctx      '{?key "key"
                                             ?uri "some://uri"}
                       :workflows/template :artifacts/create
                       :workflows/form     (peek (wf/load! :artifacts/create))
                       :workflows/->result '{:artifact/id       (sp.ctx/get ?artifact-id)
                                             :artifact/filename (sp.ctx/get ?filename)}}
                      params)
          (assert/is? {:user/id     user-id
                       :request/id  request-id
                       :workflow/id uuid?}
                      ctx))))))

(deftest query-many-test
  (testing "query-many"
    (let [[project-id user-id] (repeatedly uuids/random)
          tx (ts/->tx)
          repo (rfiles/->FileAccessor tx nil nil nil nil)]
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
                                    [[:max :file-versions.created-at] :created-at]}
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
          tx (ts/->tx)
          repo (rfiles/->FileAccessor tx nil nil nil nil)]
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
          store (ts/->store)
          tx (ts/->tx)
          repo (rfiles/->FileAccessor tx store nil nil nil)]
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
                       [:artifacts.content-length "artifact/content-length"]
                       [:artifacts.created-at "artifact/created-at"]
                       [:artifacts.key "artifact/key"]}
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
    (let [producer (ts/->chan)
          tx (ts/->tx)
          repo (rfiles/->FileAccessor tx nil producer nil nil)
          [request-id user-id] (repeatedly uuids/random)]
      (testing "when the user has access"
          (stubs/use! tx :execute! [{}])
        (testing "emits a command"
          (int/create-file! repo {:some :data} {:some       :opts
                                                :some/other :opts
                                                :user/id    user-id
                                                :request/id request-id})
          (let [[{[tag params ctx] :value}] (colls/only! (stubs/calls producer :send!))]
            (is (= ::sp.ktop/create! tag))
            (assert/is? {:workflows/ctx      {}
                         :workflows/template :files/create
                         :workflows/form     (peek (wf/load! :files/create))
                         :workflows/->result '{:file-version/id (sp.ctx/get ?version-id)
                                               :file/id         (sp.ctx/get ?file-id)}}
                        params)
            (assert/is? {:user/id     user-id
                         :request/id  request-id
                         :workflow/id uuid?}
                        ctx))))

      (testing "when the user does not have access"
        (stubs/use! tx :execute! [])
        (testing "throws"
          (let [ex (is (thrown? Throwable (int/create-file! repo {:some :data} {:some       :opts
                                                                                :some/other :opts
                                                                                :user/id    user-id
                                                                                :request/id request-id})))]
            (is (= int/NO_ACCESS (:interactor/reason (ex-data ex))))))))))

(deftest create-file-version-test
  (testing "create-file-version"
    (let [producer (ts/->chan)
          tx (ts/->tx)
          repo (rfiles/->FileAccessor tx nil producer nil nil)
          [request-id user-id] (repeatedly uuids/random)]
      (testing "when the user has access"
        (stubs/use! tx :execute! [{}])
        (testing "emits a command"
          (int/create-file-version! repo
                                    {:some :data}
                                    {:some       :opts
                                     :some/other :opts
                                     :user/id    user-id
                                     :request/id request-id})
          (let [[{[tag params ctx] :value}] (colls/only! (stubs/calls producer :send!))]
            (is (= ::sp.ktop/create! tag))
            (assert/is? {:workflows/ctx      {}
                         :workflows/template :versions/create
                         :workflows/form     (peek (wf/load! :versions/create))
                         :workflows/->result '{:file-version/id (sp.ctx/get ?version-id)}}
                        params)
            (assert/is? {:user/id     user-id
                         :request/id  request-id
                         :workflow/id uuid?}
                        ctx))))

      (testing "when the user does not have access"
        (stubs/use! tx :execute! [])
        (testing "throws"
          (let [ex (is (thrown? Throwable (int/create-file-version! repo
                                                                    {:some :data}
                                                                    {:some       :opts
                                                                     :some/other :opts
                                                                     :user/id    user-id
                                                                     :request/id request-id})))]
            (is (= int/NO_ACCESS (:interactor/reason (ex-data ex))))))))))
