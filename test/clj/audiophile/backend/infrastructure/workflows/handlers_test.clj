(ns ^:unit audiophile.backend.infrastructure.workflows.handlers-test
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.backend.infrastructure.workflows.handlers :as wfh]
    [audiophile.common.core.serdes.protocols :as pserdes]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils.assertions :as assert]
    [audiophile.test.utils.services :as ts]
    [audiophile.test.utils.stubs :as stubs]
    [clojure.test :refer [are deftest is testing]]
    [spigot.core :as sp]
    [spigot.impl.api :as spapi]
    [spigot.runner :as spr]))

(defn ^:private ->query-fn [spec]
  (fn [query _]
    (letfn [(matches? [[k v]]
              (let [pred (if (fn? v) v (partial = v))]
                (pred (get query k))))]
      (or (first (for [[matcher result] spec
                       :when (every? matches? matcher)]
                   result))
          [{}]))))

(defmacro ^:private as-test [bindings [template ctx query-fn sys] & body]
  (let [wf-sym (gensym "wf")
        sys-sym (gensym "sys")]
    `(let [tx# (ts/->tx)
           pubsub# (ts/->pubsub)
           spec# (wf/workflow-spec ~template ~ctx)
           ~sys-sym (assoc ~sys :repo tx# :pubsub pubsub#)
           ~wf-sym (sp/create (:workflows/form spec#) (:workflows/ctx spec#))]
       (testing "when the workflow succeeds"
         (stubs/set-stub! tx# :execute! ~query-fn)
         (let [result# (spr/run-all ~wf-sym (partial wfh/task-handler ~sys-sym {}))
               {:keys ~(or bindings [])} {:db-calls     (map first (stubs/calls tx# :execute!))
                                          :pubsub-calls (stubs/calls pubsub# :publish!)
                                          :result       result#}]
           ~@body))

       (testing "when the workflow fails"
         (stubs/set-stub! tx# :execute! (fn [_# _#]
                                          (throw (ex-info "barf" {}))))
         (is ~(list 'thrown? 'Throwable `(spr/run-all ~wf-sym (partial wfh/task-handler ~sys-sym {}))))))))

(deftest artifacts:create-test
  (testing ":artifacts/create workflow"
    (let [[artifact-id] (repeatedly uuids/random)
          ctx {:artifact/filename     "filename"
               :artifact/content-type "audio/mp3"
               :artifact/size         123456
               :artifact/uri          "some://uri"
               :artifact/key          "some-key"}
          query-fn (->query-fn {{:insert-into :artifacts} [{:id artifact-id}]})]
      (as-test [db-calls result] [:artifacts/create ctx query-fn]
        (testing "produces a final context"
          (assert/is? {'?artifact-id artifact-id
                       '?filename    "filename"}
                      (spapi/scope result)))

        (testing "saves the artifact"
          (assert/has? {:insert-into :artifacts
                        :values      [{:filename       "filename"
                                       :content-type   "audio/mp3"
                                       :content-length 123456
                                       :uri            "some://uri"
                                       :key            "some-key"}]}
                       db-calls))))))

(deftest comments:create-test
  (testing ":comments/create workflow"
    (let [[comment-id parent-id user-id version-id] (repeatedly uuids/random)
          ctx {:comment/body            "some comment"
               :comment/selection       [0 1]
               :comment/file-version-id version-id
               :comment/comment-id      parent-id
               :user/id                 user-id}
          query-fn (->query-fn {{:insert-into :comments} [{:id comment-id}]})]
      (as-test [db-calls result] [:comments/create ctx query-fn]
        (testing "produces a final context"
          (assert/is? {'?comment-id comment-id}
                      (spapi/scope result)))

        (testing "saves the comment"
          (assert/has? {:insert-into :comments
                        :values      [{:body            "some comment"
                                       :selection       [:cast "[0,1]" :numrange]
                                       :file-version-id version-id
                                       :comment-id      parent-id
                                       :created-by      user-id}]}
                       db-calls))))))

(deftest files:create-test
  (testing ":files/create workflow"
    (let [[artifact-id file-id project-id version-id] (repeatedly uuids/random)
          ctx {:artifact/id       artifact-id
               :project/id        project-id
               :file/name         "file"
               :file-version/name "version"}
          query-fn (->query-fn {{:insert-into :files}         [{:id file-id}]
                                {:insert-into :file-versions} [{:id version-id}]})]
      (as-test [db-calls result] [:files/create ctx query-fn]
        (testing "produces a final context"
          (assert/is? {'?file-id    file-id
                       '?version-id version-id}
                      (spapi/scope result)))

        (testing "saves the file"
          (assert/has? {:insert-into :files
                        :values      [{:name       "file"
                                       :project-id project-id
                                       :idx        {:select [:%count.idx]
                                                    :from   [:files]
                                                    :where  [:= :files.project-id project-id]}}]}
                       db-calls)
          (assert/has? {:insert-into :file-versions
                        :values      [{:artifact-id artifact-id
                                       :file-id     file-id
                                       :name        "version"}]}
                       db-calls))))))

(deftest projects:create-test
  (testing ":projects/create workflow"
    (let [[project-id team-id] (repeatedly uuids/random)
          ctx {:project/team-id team-id
               :project/name    "project name"}
          query-fn (->query-fn {{:insert-into :projects} [{:id project-id}]})]
      (as-test [db-calls result] [:projects/create ctx query-fn]
        (testing "produces a final context"
          (assert/is? {'?project-id project-id}
                      (spapi/scope result)))

        (testing "saves the project"
          (assert/has? {:insert-into :projects
                        :values      [{:name    "project name"
                                       :team-id team-id}]}
                       db-calls))))))

(deftest projects:update-test
  (testing ":projects/update workflow"
    (let [[project-id user-id] (repeatedly uuids/random)
          ctx {:project/id   project-id
               :project/name "project name"
               :user/id      user-id}
          query-fn (->query-fn {{:insert-into :projects} [{:id project-id}]})]
      (as-test [db-calls pubsub-calls] [:projects/update ctx query-fn]
        (testing "saves the project"
          (assert/has? {:update :projects
                        :set    {:project/name "project name"}
                        :where  [:= :projects.id project-id]}
                       db-calls))

        (testing "publishes an event"
          (let [[topic [_ event]] (colls/only! pubsub-calls)]
            (is (= [:projects project-id] topic))
            (assert/is? {:event/type :project/updated
                         :event/data {:project/id   project-id
                                      :project/name "project name"}}
                        event)))))))

(deftest teams:create-test
  (testing ":teams/create workflow"
    (let [[team-id user-id] (repeatedly uuids/random)
          ctx {:team/name "team name"
               :team/type :COLLABORATIVE
               :user/id   user-id}
          query-fn (->query-fn {{:insert-into :teams} [{:id team-id}]})]
      (as-test [db-calls result] [:teams/create ctx query-fn]
        (testing "produces a final context"
          (assert/is? {'?team-id team-id}
                      (spapi/scope result)))

        (testing "saves the team"
          (assert/has? {:insert-into :teams
                        :values      [{:name "team name"
                                       :type [:cast "COLLABORATIVE" :team_type]}]}
                       db-calls)
          (assert/has? {:insert-into :user-teams
                        :values      [{:team-id team-id
                                       :user-id user-id}]}
                       db-calls))))))

(deftest team-invitations:create-test
  (testing ":team-invitations/create workflow"
    (let [[invite-id user-id team-id] (repeatedly uuids/random)
          ctx {:team/id    team-id
               :user/email "user@example.com"
               :user/id    user-id
               :inviter/id invite-id}
          query-fn (->query-fn {})]
      (as-test [db-calls pubsub-calls] [:team-invitations/create ctx query-fn]
        (testing "saves the invitation"
          (assert/has? {:insert-into   :team-invitations
                        :values        [{:email      "user@example.com"
                                         :team-id    team-id
                                         :invited-by invite-id}]
                        :on-conflict   [:team-id :email]
                        :do-update-set map?}
                       db-calls))

        (let [[call-1 call-2] (colls/only! 2 pubsub-calls)]
          (testing "publishes an event to teams topic"
            (let [[topic [_ event]] call-1]
              (is (= [:teams team-id] topic))
              (assert/is? {:event/type :team-invitation/created
                           :event/data {:team-invitation/team-id team-id
                                        :team-invitation/email   "user@example.com"}}
                          event)))

          (testing "publishes an event to user topic"
            (let [[topic [_ event]] call-2]
              (is (= [::ps/user user-id] topic))
              (assert/is? {:event/type :team-invitation/created
                           :event/data {:team-invitation/team-id team-id
                                        :team-invitation/email   "user@example.com"}}
                          event))))))))

(deftest team-invitations:update-test
  (testing ":team-invitations/update workflow"
    (testing "when accepting the invitation"
      (let [[invite-id user-id team-id] (repeatedly uuids/random)
            ctx {:team/id                    team-id
                 :user/id                    user-id
                 :team-invitation/email      "user@example.com"
                 :team-invitation/invited-by invite-id
                 :team-invitation/status     :ACCEPTED}
            query-fn (->query-fn {})]
        (as-test [db-calls pubsub-calls] [:team-invitations/update ctx query-fn]
          (testing "saves the invitation"
            (assert/has? {:insert-into :user-teams
                          :values      [{:user-id user-id
                                         :team-id team-id}]
                          :on-conflict [:team-id :user-id]
                          :do-nothing  []}
                         db-calls)
            (assert/has? {:update :team-invitations
                          :set    {:status [:cast "ACCEPTED" :team-invitation-status]}
                          :where
                          [:and
                           [:= :team-invitations.team-id team-id]
                           [:= :team-invitations.email "user@example.com"]
                           [:= :team-invitations.status [:cast "PENDING" :team-invitation-status]]]}
                         db-calls))

          (let [[call-1 call-2] (colls/only! 2 pubsub-calls)]
            (testing "publishes an event to teams topic"
              (let [[topic [_ event]] call-1]
                (is (= [:teams team-id] topic))
                (assert/is? {:event/type :team-invitation/updated
                             :event/data {:team-invitation/team-id team-id
                                          :team-invitation/email   "user@example.com"
                                          :team-invitation/status  :ACCEPTED}}
                            event)))

            (testing "publishes an event to user topic"
              (let [[topic [_ event]] call-2]
                (is (= [::ps/user invite-id] topic))
                (assert/is? {:event/type :team-invitation/updated
                             :event/data {:team-invitation/team-id team-id
                                          :team-invitation/email   "user@example.com"
                                          :team-invitation/status  :ACCEPTED}}
                            event)))))))

    (testing "when accepting the invitation"
      (let [[invite-id user-id team-id] (repeatedly uuids/random)
            ctx {:team/id                    team-id
                 :user/id                    user-id
                 :team-invitation/email      "user@example.com"
                 :team-invitation/invited-by invite-id
                 :team-invitation/status     :REJECTED}
            query-fn (->query-fn {})]
        (as-test [db-calls pubsub-calls] [:team-invitations/update ctx query-fn]
          (testing "saves the invitation"
            (assert/has? {:update :team-invitations
                          :set    {:status [:cast "REJECTED" :team-invitation-status]}
                          :where
                          [:and
                           [:= :team-invitations.team-id team-id]
                           [:= :team-invitations.email "user@example.com"]
                           [:= :team-invitations.status [:cast "PENDING" :team-invitation-status]]]}
                         db-calls))

          (let [[call-1 call-2] (colls/only! 2 pubsub-calls)]
            (testing "publishes an event to teams topic"
              (let [[topic [_ event]] call-1]
                (is (= [:teams team-id] topic))
                (assert/is? {:event/type :team-invitation/updated
                             :event/data {:team-invitation/team-id team-id
                                          :team-invitation/email   "user@example.com"
                                          :team-invitation/status  :REJECTED}}
                            event)))

            (testing "publishes an event to user topic"
              (let [[topic [_ event]] call-2]
                (is (= [::ps/user invite-id] topic))
                (assert/is? {:event/type :team-invitation/updated
                             :event/data {:team-invitation/team-id team-id
                                          :team-invitation/email   "user@example.com"
                                          :team-invitation/status  :REJECTED}}
                            event)))))))))

(deftest teams:update-test
  (testing ":teams/update workflow"
    (let [[team-id user-id] (repeatedly uuids/random)
          ctx {:team/id   team-id
               :team/name "team name"
               :user/id   user-id}
          query-fn (->query-fn {{:insert-into :teams} [{:id team-id}]})]
      (as-test [db-calls pubsub-calls] [:teams/update ctx query-fn]
        (testing "saves the team"
          (assert/has? {:update :teams
                        :set    {:team/name "team name"}
                        :where  [:= :teams.id team-id]}
                       db-calls))

        (testing "publishes an event"
          (let [[topic [_ event]] (colls/only! pubsub-calls)]
            (is (= [:teams team-id] topic))
            (assert/is? {:event/type :team/updated
                         :event/data {:team/id   team-id
                                      :team/name "team name"}}
                        event)))))))

(deftest users:signup-test
  (testing ":users/signup workflow"
    (let [[team-id user-id] (repeatedly uuids/random)
          ctx {:user/handle        "joeblowsville"
               :user/email         "joe.blow@for.realz"
               :user/mobile-number "9876543210"
               :user/first-name    "Joe"
               :user/last-name     "Blow"}
          query-fn (->query-fn {{:insert-into :users} [{:id user-id}]
                                {:insert-into :teams} [{:id team-id}]
                                {:select any?
                                 :from   [:users]}    []})]
      (as-test [db-calls result] [:users/signup ctx query-fn {:jwt-serde (reify
                                                                           pserdes/ISerde
                                                                           (serialize [_ _ _]
                                                                             "jwt-token"))}]
        (testing "produces a final context"
          (assert/is? {'?user-id user-id
                       '?token   "jwt-token"}
                      (spapi/scope result)))

        (testing "saves the user"
          (assert/has? {:select any?
                        :from   [:users]
                        :where  [:= :users.email "joe.blow@for.realz"]}
                       db-calls)
          (assert/has? {:select any?
                        :from   [:users]
                        :where  [:= :users.handle "joeblowsville"]}
                       db-calls)
          (assert/has? {:select any?
                        :from   [:users]
                        :where  [:= :users.mobile-number "9876543210"]}
                       db-calls)
          (assert/has? {:insert-into :users
                        :values      [{:handle        "joeblowsville"
                                       :email         "joe.blow@for.realz"
                                       :mobile-number "9876543210"
                                       :first-name    "Joe"
                                       :last-name     "Blow"}]}
                       db-calls))))))

(deftest versions:activate-test
  (testing ":file-versions/activate workflow"
    (let [[version-id] (repeatedly uuids/random)
          ctx {:file-version/id version-id}]
      (as-test [db-calls] [:file-versions/activate ctx]
        (testing "saves the file version"
          (assert/has? {:update :file-versions
                        :set    {:selected-at [:raw "now()"]}
                        :where  [:= :file-versions.id version-id]}
                       db-calls))))))

(deftest versions:create-test
  (testing ":versions/create workflow"
    (let [[artifact-id file-id version-id] (repeatedly uuids/random)
          ctx {:artifact/id       artifact-id
               :file/id           file-id
               :file-version/name "version"}
          query-fn (->query-fn {{:insert-into :file-versions} [{:id version-id}]})]
      (as-test [db-calls result] [:file-versions/create ctx query-fn]
        (testing "produces a final context"
          (assert/is? {'?version-id version-id}
                      (spapi/scope result)))

        (testing "saves the file version"
          (assert/has? {:insert-into :file-versions
                        :values      [{:artifact-id artifact-id
                                       :file-id     file-id
                                       :name        "version"}]}
                       db-calls))))))
