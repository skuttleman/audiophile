(ns test.utils.stubs
  #?(:cljs
     (:require-macros
       test.utils.stubs))
  (:require
    [com.ben-allred.audiophile.common.utils.logger :as log])
  #?(:clj
     (:import
       (clojure.lang IDeref))))

(defprotocol IStub
  (init! [this] "set all stub behavior to initial behavior and reset calls")
  (-set-stub! [this method f-or-val] "update an individual stub")
  (-get-stub [this method] "return the current stub (if any) for a method")
  (-calls [this]))

(defn ^:private ->method [method f state n]
  (let [args (into [] (repeatedly n gensym))]
    (list method args
          (list* f state (rest args))
          (first args))))

(defmacro create
  "Takes a reify expression and adds mixes in an [[IStub]] implementation to be
   used with the functions in this namespace.

   ```clojure
   (create (reify
             MyInterface
             (method [_ _ _]))
   ```"
  [reify]
  (let [deref (if (:ns &env) '-deref 'deref)
        state (gensym)
        stub (-> (map (fn [x]
                        (if (list? x)
                          (let [[method args & body] x
                                args' (mapv (fn [_] (gensym)) args)
                                bindings (into [] (mapcat vector args args'))
                                f-args (rest args')
                                k (keyword method)]
                            `(~method ~args'
                               (let [state# @~state]
                                 (swap! ~state update-in [:calls ~k] (fnil conj []) [~@f-args])
                                 (if-let [f# (get-in state# [:stubs ~k])]
                                   (f# ~@f-args)
                                   (let ~bindings ~@body)))))
                          x))
                      reify)
                 (concat
                   (list `IStub
                         (let [sym (gensym)]
                           (list 'init! [sym]
                                 (list `swap! state `dissoc :stubs :calls)
                                 sym))
                         (let [[this method val] (repeatedly gensym)]
                           (list '-set-stub! [this method val]
                                 (list `swap! state `update :stubs `assoc method val)))
                         (let [[this method] (repeatedly gensym)]
                           (list '-get-stub [this method]
                                 (list `get-in (list `deref state) [:stubs method])))
                         (list '-calls '[_]
                               (list :calls (list `deref state))))))]
    `(let [~state (atom {:stubs nil})]
       ~stub)))

(defn stub? [x]
  (satisfies? IStub x))

(defn set-stub! [stub method f-or-val]
  (-set-stub! stub method (if (fn? f-or-val) f-or-val (constantly f-or-val)))
  stub)

(defn remove-stub! [stub method]
  (-set-stub! stub method nil)
  stub)

(defn calls [stub method]
  (get (-calls stub) method))

(defn use! [stub method & vals]
  (when (seq vals)
    (let [results (atom vals)
          curr (-get-stub stub method)]
      (-set-stub! stub method (fn [& args]
                                (let [result (first @results)]
                                  (when (empty? (swap! results rest))
                                    (-set-stub! stub method curr))
                                  (cond
                                    (instance? #?(:cljs js/Error :default Throwable) result)
                                    (throw result)

                                    (fn? result)
                                    (apply result args)

                                    :else
                                    result))))))
  stub)
