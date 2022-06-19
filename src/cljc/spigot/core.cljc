(ns spigot.core
  (:refer-clojure :exclude [next])
  (:require
    [spigot.impl :as sp.impl]))

(defn plan [form]
  (sp.impl/create form))

(defn next
  "Invokes all runnable tasks with executor and returns an updated workflow."
  [workflow executor]
  (sp.impl/next workflow executor))

(defn finish
  "processes a finished task and returns an updated workflow."
  [workflow id result]
  (sp.impl/finish workflow id result))

(defn run
  "Reduces through a workflow, starting and finishing tasks in dependency order.
   `executor` should be a function of `task` -> `result`."
  [workflow executor]
  (loop [wf workflow]
    (if (seq (remove (:completed wf) (keys (:deps wf))))
      (let [tasks (atom [])]
        (recur (reduce (fn [wf' {:spigot/keys [id] :as task}]
                         (finish wf' id (sp.impl/execute task (:ctx wf') executor)))
                       (next wf (partial swap! tasks conj))
                       @tasks)))
      wf)))
