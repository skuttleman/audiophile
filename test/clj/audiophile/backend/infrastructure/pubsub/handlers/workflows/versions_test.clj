(ns ^:unit audiophile.backend.infrastructure.pubsub.handlers.workflows.versions-test
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils.assertions :as assert]
    [audiophile.test.utils.stubs :as stubs]
    [audiophile.test.utils.workflows :as twf]
    [clojure.test :refer [are deftest is testing]]
    audiophile.backend.infrastructure.pubsub.handlers.files))

(deftest wf:create-test
  (testing ":versions/create workflow"
    (twf/with-setup [commands db events tx]
      (testing "when the workflow succeeds"
        (let [[artifact-id file-id user-id version-id] (repeatedly uuids/random)]
          (stubs/set-stub! tx :execute! (twf/->mem-db db (fn [query _]
                                                           (when (= :file-versions (:insert-into query))
                                                             [{:id version-id}]))))
          (let [{:event/keys [model-id] :as event} (twf/with-event [events]
                                                     (ps/start-workflow! commands
                                                                         :versions/create
                                                                         {:artifact/id  artifact-id
                                                                          :file/id      file-id
                                                                          :version/name "version"}
                                                                         {:user/id user-id}))
                db-calls (map first (stubs/calls tx :execute!))]
            (testing "saves the file version"
              (assert/has? {:insert-into :file-versions
                            :values      [{:artifact-id artifact-id
                                           :file-id     file-id
                                           :name        "version"}]}
                           db-calls))

            (testing "emits an event"
              (assert/is? {:event/id         uuid?
                           :event/model-id   uuid?
                           :event/type       :workflow/completed
                           :event/emitted-by user-id}
                          event)
              (assert/is? {:file-version/id version-id}
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
