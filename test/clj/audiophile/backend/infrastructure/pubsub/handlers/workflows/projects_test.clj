(ns ^:unit audiophile.backend.infrastructure.pubsub.handlers.workflows.projects-test
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils.assertions :as assert]
    [audiophile.test.utils.stubs :as stubs]
    [audiophile.test.utils.workflows :as twf]
    [clojure.test :refer [are deftest is testing]]
    audiophile.backend.infrastructure.pubsub.handlers.projects))

(deftest wf:create-test
  (testing ":projects/create workflow"
    (twf/with-setup [commands db events tx]
      (testing "when the workflow succeeds"
        (let [[project-id team-id user-id] (repeatedly uuids/random)]
          (stubs/set-stub! tx :execute! (twf/->mem-db db (fn [query _]
                                                           (when (= :projects (:insert-into query))
                                                             [{:id project-id}]))))
          (let [{:event/keys [model-id] :as event} (twf/with-event [events]
                                                     (ps/start-workflow! commands
                                                                         :projects/create
                                                                         {:project/team-id team-id
                                                                          :project/name    "project name"}
                                                                         {:user/id user-id}))
                db-calls (map first (stubs/calls tx :execute!))]
            (testing "saves the project"
              (assert/has? {:insert-into :projects
                            :values      [{:name    "project name"
                                           :team-id team-id}]}
                           db-calls))

            (testing "emits an event"
              (assert/is? {:event/id         uuid?
                           :event/model-id   uuid?
                           :event/type       :workflow/completed
                           :event/emitted-by user-id}
                          event)
              (assert/is? {:project/id project-id}
                          (:event/data event))
              (is (= model-id (-> event :event/ctx :workflow/id)))))))

      (testing "when the workflow fails"
        (stubs/set-stub! tx :execute! (twf/->mem-db db (fn [query _]
                                                         (when (= :workflows (:update query))
                                                           (throw (ex-info "barf" {}))))))
        (let [[user-id] (repeatedly uuids/random)
              {:event/keys [model-id] :as event} (twf/with-event [events]
                                                   (ps/start-workflow! commands
                                                                       :files/create
                                                                       {:file/filename     "filename"
                                                                        :file/content-type "audio/mp3"
                                                                        :file/size         123456
                                                                        :file/uri          "some://uri"
                                                                        :file/key          "some-key"}
                                                                       {:user/id user-id}))]
          (testing "emits an event"
            (assert/is? {:event/id         uuid?
                         :event/model-id   uuid?
                         :event/type       :command/failed
                         :event/data       {:error/command :workflow/create!
                                            :error/reason  "barf"}
                         :event/emitted-by user-id}
                        event)
            (is (= model-id (-> event :event/ctx :workflow/id)))))))))
