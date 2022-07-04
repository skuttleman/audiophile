(ns spigot.core
  (:refer-clojure :exclude [next])
  (:require
    [spigot.impl :as sp.impl]))

(defn plan
  ([form]
   (plan form nil))
  ([form opts]
   (sp.impl/create form opts)))

(defn next
  "Invokes all runnable tasks with executor and returns an updated workflow."
  [workflow executor]
  (sp.impl/next workflow executor))

(defn next-sync
  "Returns a tuple of [`updated-workflow` `set-of-tasks`]"
  [workflow]
  (let [tasks (transient #{})
        updated-workflow (next workflow (partial conj! tasks))]
    [updated-workflow (persistent! tasks)]))

(defn finish
  "processes a finished task and returns an updated workflow."
  [workflow id result]
  (sp.impl/finish workflow id result))

(defn finished?
  "have all tasks been completed?"
  [workflow]
  (boolean (sp.impl/finished? workflow)))
