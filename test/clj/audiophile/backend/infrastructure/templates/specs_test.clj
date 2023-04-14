(ns ^:unit audiophile.backend.infrastructure.templates.specs-test
  (:require
    [audiophile.backend.infrastructure.templates.specs :as wfs]
    [audiophile.backend.infrastructure.templates.workflows :as wf]
    [audiophile.common.core.utils.logger :as log]
    [clojure.test :refer [are deftest is testing]]
    [malli.core :as m]
    [malli.generator :as mg]
    [spigot.core :as sp]
    [spigot.impl.api :as spapi]
    [spigot.impl.context :as spc]
    [spigot.runner :as spr]))

(defn ^:private spec-check!
  "Given a workflow spec, generates all inputs and outputs per defined specs to verify the output"
  [template input]
  (let [ret-spec (wfs/ret-spec template)
        wf (wf/workflow-spec template input)
        result (-> (:workflows/form wf)
                   (sp/create (:workflows/ctx wf))
                   (spr/run-all (fn [[tag params]]
                                   (let [param-spec (wfs/param-spec tag)
                                         ret-spec (wfs/ret-spec tag)]
                                     (when (and param-spec (not (m/validate param-spec params)))
                                       (throw (ex-info "invalid task input" {:task        [tag params]
                                                                             :explanation (m/explain param-spec params)})))
                                     (some-> ret-spec mg/generate)))))
        output (spc/resolve-into (:workflows/->result wf) (spapi/scope result))]
    (when (and ret-spec (not (m/validate ret-spec output)))
      (throw (ex-info "invalid workflow output" {:output      output
                                                 :explanation (m/explain ret-spec output)})))))

(defn ^:private completable?
  ([template]
   (completable? template (mg/generate (wfs/param-spec template))))
  ([template input]
   (try (spec-check! template input)
        true
        (catch Throwable ex
          (log/error ex "workflow could not be completed")
          false))))

(deftest workflow-completable-test
  (testing "workflows are completable"
    (are [template] (completable? template)
      :artifacts/create
      :comments/create
      :file-versions/activate
      :file-versions/create
      :files/create
      :files/update
      :projects/create
      :projects/update
      :team-invitations/create
      :team-invitations/update
      :teams/create
      :teams/update
      :users/signup)))
