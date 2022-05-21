(ns ^:unit audiophile.backend.api.pubsub.core-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.test.utils.assertions :as assert]
    [audiophile.test.utils.services :as ts]
    [audiophile.test.utils.stubs :as stubs]))

(deftest emit-event!-test
  (testing "(emit-event!)"
    (let [ch (ts/->chan)
          [user-id model-id request-id] (repeatedly uuids/random)
          event-id (ps/emit-event! ch model-id :event/type {:some :data} {:request/id request-id
                                                                          :user/id    user-id})]
      (testing "publishes an event"
        (let [event (-> ch
                        (stubs/calls :send!)
                        colls/only!
                        first)]
          (assert/is? {:event/id         event-id
                       :event/model-id   model-id
                       :event/type       :event/type
                       :event/data       {:some :data}
                       :event/emitted-by user-id
                       :event/ctx        {:request/id request-id
                                          :user/id    user-id}}
                      event))))))

(deftest emit-command!-test
  (testing "(emit-command!)"
    (let [ch (ts/->chan)
          [user-id request-id] (repeatedly uuids/random)
          command-id (ps/emit-command! ch :command/type {:some :data} {:request/id request-id
                                                                       :user/id    user-id})]
      (testing "publishes an event"
        (let [command (-> ch
                          (stubs/calls :send!)
                          colls/only!
                          first)]
          (assert/is? {:command/id         command-id
                       :command/type       :command/type
                       :command/data       {:some :data}
                       :command/emitted-by user-id
                       :command/ctx        {:request/id request-id
                                            :user/id    user-id}}
                      command))))))

(deftest command-failed!-test
  (testing "(command-failed!)"
    (let [ch (ts/->chan)
          [user-id model-id request-id] (repeatedly uuids/random)
          event-id (ps/command-failed! ch model-id {:request/id    request-id
                                                    :user/id       user-id
                                                    :error/command :some/command!
                                                    :error/reason  "reason"})]
      (testing "publishes an event"
        (let [event (-> ch
                        (stubs/calls :send!)
                        colls/only!
                        first)]
          (assert/is? {:event/id         event-id
                       :event/model-id   model-id
                       :event/type       :command/failed
                       :event/data       {:error/command :some/command!
                                          :error/reason  "reason"}
                       :event/emitted-by user-id
                       :event/ctx        {:request/id request-id
                                          :user/id    user-id}}
                      event))))))
