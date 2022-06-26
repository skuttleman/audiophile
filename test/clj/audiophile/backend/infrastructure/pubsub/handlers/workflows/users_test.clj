(ns ^:unit audiophile.backend.infrastructure.pubsub.handlers.workflows.users-test
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils.assertions :as assert]
    [audiophile.test.utils.stubs :as stubs]
    [audiophile.test.utils.workflows :as twf]
    [clojure.set :as set]
    [clojure.test :refer [are deftest is testing]]
    audiophile.backend.infrastructure.pubsub.handlers.teams
    audiophile.backend.infrastructure.pubsub.handlers.users))

(deftest wf:signup-test
  (testing ":users/signup workflow"
    (twf/with-setup [commands db events jwt-serde tx]
      (testing "when the workflow succeeds"
        (let [[signup-id team-id user-id] (repeatedly uuids/random)]
          (stubs/set-stub! tx :execute! (twf/->mem-db db (fn [query _]
                                                           (case (:insert-into query)
                                                             :users [{:id user-id}]
                                                             :teams [{:id team-id}]
                                                             nil))))
          (let [{:event/keys [model-id] :as event} (twf/with-event [events]
                                                     (ps/start-workflow! commands
                                                                         :users/signup
                                                                         {:user/handle        "joeblowsville"
                                                                          :user/email         "joe.blow@for.realz"
                                                                          :user/mobile-number "9876543210"
                                                                          :user/first-name    "Joe"
                                                                          :user/last-name     "Blow"}
                                                                         {:user/id signup-id}))
                db-calls (map first (stubs/calls tx :execute!))
                token-data (serdes/deserialize jwt-serde (-> event :event/data :login/token))]
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
                           db-calls))

            (testing "saves the team"
              (assert/has? {:insert-into :teams
                            :values      [{:name "My Personal Projects",
                                           :type [:cast "PERSONAL" :team_type]}]}
                           db-calls)
              (assert/has? {:insert-into :user-teams
                            :values      [{:user-id user-id
                                           :team-id team-id}]}
                           db-calls))

            (testing "emits an event"
              (assert/is? {:event/id         uuid?
                           :event/model-id   uuid?
                           :event/type       :workflow/completed
                           :event/emitted-by signup-id}
                          event)
              (is (= model-id (-> event :event/ctx :workflow/id)))
              (assert/is? {:user/handle        "joeblowsville"
                           :user/email         "joe.blow@for.realz"
                           :user/mobile-number "9876543210"
                           :user/first-name    "Joe"
                           :user/last-name     "Blow"}
                          token-data)
              (is (set/superset? (:jwt/aud token-data) #{:token/login}))))))

      (testing "when the workflow fails"
        (stubs/set-stub! tx :execute! (twf/->mem-db db (fn [query _]
                                                         (when (= :workflows (:update query))
                                                           (throw (ex-info "barf" {}))))))
        (let [[signup-id] (repeatedly uuids/random)
              {:event/keys [model-id] :as event} (twf/with-event [events]
                                                   (ps/start-workflow! commands
                                                                       :users/signup
                                                                       {:user/handle        "joeblowsville"
                                                                        :user/email         "joe.blow@for.realz"
                                                                        :user/mobile-number "9876543210"
                                                                        :user/first-name    "Joe"
                                                                        :user/last-name     "Blow"}
                                                                       {:user/id signup-id}))]
          (testing "emits an event"
            (assert/is? {:event/id         uuid?
                         :event/model-id   uuid?
                         :event/type       :command/failed
                         :event/data       {:error/command :workflow/create!
                                            :error/reason  "barf"}
                         :event/emitted-by signup-id}
                        event)
            (is (= model-id (-> event :event/ctx :workflow/id)))))))))
