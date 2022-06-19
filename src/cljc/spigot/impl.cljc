(ns spigot.impl
  (:refer-clojure :exclude [next])
  (:require
    [spigot.context :as sp.ctx])
  #?(:clj
     (:import (java.util UUID))))

(defn ^:private gen-id []
  #?(:clj  (UUID/randomUUID)
     :cljs (random-uuid)))

(defn ^:private force-opts [[tag & more]]
  (let [[opts & children] (cond->> more
                            (not (map? (first more))) (cons {}))]
    (into [tag opts] (map force-opts) children)))

(defmulti build-deps
          (fn [_ {:spigot/keys [tag]}]
            tag))

(defmethod build-deps :spigot/serial
  [[deps agg] {:spigot/keys [children]}]
  (reduce build-deps
          [deps agg]
          children))

(defmethod build-deps :spigot/parallel
  [[deps agg] {:spigot/keys [children]}]
  (reduce (fn [[deps' agg'] child]
            (update (build-deps [deps agg'] child)
                    0
                    (partial into deps')))
          [#{} agg]
          children))

(defmethod build-deps :default
  [[deps agg] {:spigot/keys [id] :as task}]
  [#{id} (-> agg
             (assoc-in [:deps id] deps)
             (assoc-in [:tasks id] task))])

(defn ^:private normalize-tree [[tag opts & children]]
  (let [params (not-empty (dissoc opts :spigot/->ctx))]
    (-> opts
        (select-keys #{:spigot/->ctx})
        (assoc :spigot/id (gen-id)
               :spigot/tag tag)
        (cond->
          params (assoc :spigot/params params)
          (seq children) (assoc :spigot/children (map normalize-tree children))))))

(defn ^:private runnable [workflow]
  (if (seq (:running workflow))
    #{}
    (into #{}
          (comp (filter (comp empty? val))
                (map key)
                (remove (:completed workflow)))
          (:remaining workflow))))

(defn execute [task ctx executor]
  (-> task
      (select-keys #{:spigot/id :spigot/params :spigot/tag})
      (update :spigot/params sp.ctx/resolve-params ctx)
      executor))

(defn next [this executor]
  (let [ids (runnable this)]
    (reduce (fn [workflow id]
              (-> workflow
                  (get-in [:tasks id])
                  (execute (:ctx workflow) executor))
              (-> workflow
                  (update :running conj id)
                  (update :remaining (partial reduce (fn [rm [k v]]
                                                       (assoc rm k (disj v id)))
                                              {}))))
            this
            ids)))

(defn finish [this id result]
  (if-let [{:spigot/keys [->ctx]} (get-in this [:tasks id])]
    (-> this
        (update :running disj id)
        (update :completed conj id)
        (update :ctx sp.ctx/merge-ctx ->ctx result))
    (throw (ex-info "unknown task" {:id id}))))

(defn create [form]
  (let [workflow (->> form
                      force-opts
                      normalize-tree
                      (build-deps [#{} {:deps {} :tasks {}}])
                      second)]
    (assoc workflow
           :ctx {}
           :running #{}
           :completed #{}
           :remaining (:deps workflow))))
