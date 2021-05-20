(ns test.utils.stubs
  #?(:cljs
     (:require-macros
       test.utils.stubs))
  (:require
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [test.utils.protocols :as pu])
  #?(:clj
     (:import
       (clojure.lang IDeref))))

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
                   [`pu/IInit
                    (list* 'init! `([this#]
                                    (swap! ~state dissoc :stubs :calls)
                                    this#))
                    `pu/IStub
                    (list* 'set-stub! `([this# method# val#]
                                         (swap! ~state update :stubs assoc method# val#)))
                    (list* 'get-stub `([this# method#]
                                        (get-in (deref ~state) [:stubs method#])))
                    `pu/IReport
                    (list* 'calls `([this#]
                                     (:calls (deref ~state))))]))]
    `(let [~state (atom {:stubs nil})]
       ~stub)))

(defn stub? [x]
  (satisfies? pu/IStub x))

(defn set-stub! [stub method f-or-val]
  (pu/set-stub! stub method (if (fn? f-or-val) f-or-val (constantly f-or-val)))
  stub)

(defn remove-stub! [stub method]
  (pu/set-stub! stub method nil)
  stub)

(defn calls [stub method]
  (get (pu/calls stub) method))

(defn use! [stub method & vals]
  (when (seq vals)
    (let [results (atom vals)
          curr (pu/get-stub stub method)]
      (pu/set-stub! stub method (fn [& args]
                                (let [result (first @results)]
                                  (when (empty? (swap! results rest))
                                    (pu/set-stub! stub method curr))
                                  (cond
                                    (instance? #?(:cljs js/Error :default Throwable) result)
                                    (throw result)

                                    (fn? result)
                                    (apply result args)

                                    :else
                                    result))))))
  stub)

(defn init! [stub]
  (pu/init! stub))
