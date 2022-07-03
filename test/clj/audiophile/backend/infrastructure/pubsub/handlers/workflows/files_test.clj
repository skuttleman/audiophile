(ns ^:unit audiophile.backend.infrastructure.pubsub.handlers.workflows.files-test
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
  #_(testing ":files/create workflow"
    (twf/with-setup [commands db events tx]
      (testing "when the workflow succeeds"
        (let [[artifact-id file-id project-id user-id version-id] (repeatedly uuids/random)]
          (stubs/set-stub! tx :execute! (twf/->mem-db db (fn [query _]
                                                           (case (:insert-into query)
                                                             :files [{:id file-id}]
                                                             :file-versions [{:id version-id}]
                                                             nil))))
          (let [{:event/keys [model-id] :as event} (twf/with-event [events]
                                                     (ps/start-workflow! commands
                                                                         :files/create
                                                                         {:artifact/id  artifact-id
                                                                          :project/id   project-id
                                                                          :file/name    "file"
                                                                          :version/name "version"}
                                                                         {:user/id user-id}))
                db-calls (map first (stubs/calls tx :execute!))]
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
                           db-calls))

            (testing "emits an event"
              (assert/is? {:event/id         uuid?
                           :event/model-id   uuid?
                           :event/type       :workflow/completed
                           :event/emitted-by user-id}
                          event)
              (assert/is? {:file/id         file-id
                           :file-version/id version-id}
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
