(ns ^:unit audiophile.backend.infrastructure.pubsub.handlers.workflows.artifacts-test
  (:require
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils.assertions :as assert]
    [audiophile.test.utils.stubs :as stubs]
    [audiophile.test.utils.workflows :as twf]
    [clojure.test :refer [are deftest is testing]]
    audiophile.backend.infrastructure.pubsub.handlers.files))

(deftest wf:create-test
  (is true)
  ;; TODO FIXME
  #_(testing ":artifacts/create workflow"
    (twf/with-setup [commands db events tx]
      (testing "when the workflow succeeds"
        (let [[artifact-id user-id] (repeatedly uuids/random)]
          (stubs/set-stub! tx :execute! (twf/->mem-db db (fn [query _]
                                                           (when (= :artifacts (:insert-into query))
                                                             [{:id artifact-id}]))))
          (let [{:event/keys [model-id] :as event} (twf/with-event [events]
                                                     (ps/start-workflow! commands
                                                                         :artifacts/create
                                                                         {:artifact/filename     "filename"
                                                                          :artifact/content-type "audio/mp3"
                                                                          :artifact/size         123456
                                                                          :artifact/uri          "some://uri"
                                                                          :artifact/key          "some-key"}
                                                                         {:user/id user-id}))
                db-calls (map first (stubs/calls tx :execute!))]
            (testing "saves the artifact"
              (assert/has? {:insert-into :artifacts
                            :values      [{:filename       "filename"
                                           :content-type   "audio/mp3"
                                           :content-length 123456
                                           :uri            "some://uri"
                                           :key            "some-key"}]}
                           db-calls))

            (testing "emits an event"
              (assert/is? {:event/id         uuid?
                           :event/model-id   uuid?
                           :event/type       :workflow/completed
                           :event/emitted-by user-id}
                          event)
              (assert/is? {:artifact/id       artifact-id
                           :artifact/filename "filename"}
                          (:event/data event))
              (is (= model-id (-> event :event/ctx :workflow/id)))))))

      (testing "when the workflow fails"
        (stubs/set-stub! tx :execute! (twf/->mem-db db (fn [query _]
                                                         (when (= :workflows (:update query))
                                                           (throw (ex-info "barf" {}))))))
        (let [[user-id] (repeatedly uuids/random)
              {:event/keys [model-id] :as event} (twf/with-event [events]
                                                   (ps/start-workflow! commands
                                                                       :artifacts/create
                                                                       {:artifact/filename     "filename"
                                                                        :artifact/content-type "audio/mp3"
                                                                        :artifact/size         123456
                                                                        :artifact/uri          "some://uri"
                                                                        :artifact/key          "some-key"}
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
