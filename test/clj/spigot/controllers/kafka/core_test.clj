(ns ^:integration spigot.controllers.kafka.core-test
  (:require
    [clojure.test :refer [are deftest is testing]]
    [spigot.controllers.kafka.common :as sp.kcom]
    [spigot.controllers.protocols :as sp.pcon]
    [spigot.controllers.kafka.core :as sp.kafka]
    [spigot.core :as sp]
    [spigot.impl.api :as spapi]
    [spigot.runner :as spr])
  (:import
    (java.util UUID)
    (org.apache.kafka.streams TopologyTestDriver TestInputTopic TestOutputTopic)))

(defn ^:private setup [handler]
  (let [opts (into {:handler handler}
                   (map (juxt (comp keyword #(str % "-cfg"))
                              sp.kcom/->topic-cfg))
                   ["event-topic"
                    "task-topic"
                    "workflow-topic"])
        driver (-> (sp.kafka/default-builder)
                   (sp.kafka/with-wf-topology opts)
                   (sp.kafka/with-task-topology opts)
                   .build
                   (TopologyTestDriver. (sp.kcom/->props {:application.id    (str (UUID/randomUUID))
                                                          :bootstrap.servers "fake"})))]
    {:driver    driver
     :events    (.createOutputTopic driver
                                    "event-topic"
                                    (sp.kcom/->EdnDeserializer)
                                    (sp.kcom/->EdnDeserializer))
     :workflows (.createInputTopic driver
                                   "workflow-topic"
                                   (sp.kcom/->EdnSerializer)
                                   (sp.kcom/->EdnSerializer))}))

(deftype TestHandler [fns]
  sp.pcon/IWorkflowHandler
  (on-create [_ ctx wf]
    (if-let [f (get fns :create)]
      (f ctx wf)
      [::create ctx wf]))
  (on-update [_ ctx wf]
    (if-let [f (get fns :update)]
      (f ctx wf)
      [::update ctx wf]))
  (on-complete [_ ctx wf]
    (if-let [f (get fns :complete)]
      (f ctx wf)
      [::complete ctx wf]))

  sp.pcon/IErrorHandler
  (on-error [_ ctx wf]
    (if-let [f (get fns :error)]
      (f ctx wf)
      [::error ctx wf]))

  sp.pcon/ITaskProcessor
  (process-task [_ ctx task]
    (if-let [f (get fns :task)]
      (f ctx task)
      {:result (second task)})))

(defn ->test-handler
  ([]
   (->test-handler {}))
  ([m]
   (->TestHandler m)))

(defn ^:private thrower [_ _]
  (throw (Exception. "bad")))

(defmacro ^:private with-driver [bindings handler & body]
  `(let [{:keys ~bindings driver# :driver} (setup ~handler)]
     (try ~@body
          (finally
            (.close driver#)))))

(defn ->executor [handler]
  (partial sp.pcon/process-task handler nil))

(deftest topology-complete-test
  (let [handler (->test-handler)
        wf-id (UUID/randomUUID)]
    (with-driver [^TestInputTopic workflows ^TestOutputTopic events] handler
      (testing "matches the sync-run result"
        (are [input] (let [wf (sp/create input)
                           [_ _ result] (do (.pipeInput workflows wf-id (sp.kafka/create-wf-msg wf {}))
                                            (.-value (last (.readKeyValuesToList events))))
                           local (spr/run-all wf (->executor handler))]
                       (and (= (sp/status local)
                               (sp/status result))
                            (= (spapi/scope local)
                               (spapi/scope result))))
          [:task-1]

          [:spigot/parallel
           [:task-1]
           [:spigot/serial
            [:task-2]
            [:task-3]]]

          '[:spigot/serial
            [:spigot/parallel
             [:task-1 {:spigot/->ctx {?task-1 :result}}]
             [:task-2 {:spigot/->ctx {?task-2 :result}}]]
            [:task-3 {:task-1-result (spigot/get ?task-1)
                      :task-2-result (spigot/get ?task-2)}]])))))

(deftest topology-success-test
  (let [handler (->test-handler)
        wf-id (UUID/randomUUID)
        wf (sp/create [:spigot/parallel
                       [:task-1]
                       [:spigot/serial
                        [:task-2]
                        [:task-3]]])]
    (with-driver [^TestInputTopic workflows ^TestOutputTopic events] handler
      (.pipeInput workflows wf-id (sp.kafka/create-wf-msg wf {:some :ctx}))
      (let [[create update-1 update-2 complete] (map sp.kcom/->kv-pair
                                                     (.readKeyValuesToList events))]
        (testing "produces a create event"
          (let [[id [type ctx]] create]
            (is (= wf-id id))
            (is (= ::create type))
            (is (= {:some :ctx} ctx))))

        (testing "produces update events for each phase"
          (doseq [event [update-1 update-2]
                  :let [[id [type ctx]] event]]
            (is (= wf-id id))
            (is (= ::update type))
            (is (= {:some :ctx} ctx))))

        (testing "produces a complete event"
          (let [[id [type ctx]] complete]
            (is (= wf-id id))
            (is (= ::complete type))
            (is (= {:some :ctx} ctx))))))))

(deftest topology-task-fails-test
  (let [handler (->test-handler {:task thrower})
        wf-id (UUID/randomUUID)
        wf (sp/create [:spigot/serial
                       [:task-1]])]
    (with-driver [^TestInputTopic workflows ^TestOutputTopic events] handler
      (.pipeInput workflows wf-id (sp.kafka/create-wf-msg wf {:some :ctx}))
      (let [[_ error :as events] (map sp.kcom/->kv-pair (.readKeyValuesToList events))]
        (testing "produces an error event"
          (let [[id [type ctx]] error]
            (is (= wf-id id))
            (is (= ::error type))
            (is (= {:some :ctx} ctx))))

        (testing "produces no more events"
          (is (= 2 (count events))))))))
