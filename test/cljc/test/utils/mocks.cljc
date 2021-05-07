(ns test.utils.mocks
  (:require
    [com.ben-allred.audiophile.common.utils.logger :as log])
  (:import
    (clojure.lang IAtom IDeref)))

(defprotocol IMock
  (init! [this] "set all mock behavior to initial behavior and reset calls")
  (-set-mock! [this method f-or-val] "update an individual mock")
  (-get-mock [this method] "return the current mock (if any) for a method"))

(defn ^:private ->method [method f state n]
  (let [args (into [] (repeatedly n gensym))]
    (list method args
          (list* f state (rest args))
          (first args))))

(defmacro ->mock
  "takes a reify expression and an optional map of default mocks and generates a mutable mocking implementation"
  ([reify]
   `(->mock nil ~reify))
  ([defaults reify]
   (let [deref (if (:ns &env) '-deref 'deref)
         state (gensym)
         mock (-> (map (fn [x]
                         (if (list? x)
                           (let [[method args & body] x
                                 args' (mapv (fn [_] (gensym)) args)
                                 bindings (into [] (mapcat vector args args'))
                                 f-args (rest args')
                                 k (keyword method)]
                             `(~method ~args'
                                (let [state# @~state]
                                  (swap! ~state update-in [:calls ~k] (fnil conj []) [~@f-args])
                                  (if-let [f# (get-in state# [:mocks ~k])]
                                    (f# ~@f-args)
                                    (let ~bindings ~@body)))))
                           x))
                       reify)
                  (concat
                    (list `IDeref
                          (list deref '[_]
                                (list :calls (list `deref state)))
                          `IMock
                          (let [sym (gensym)]
                            (list 'init! [sym]
                                  (list `swap! state `assoc :mocks defaults :calls nil)
                                  sym))
                          (let [[this method val] (repeatedly gensym)]
                            (list '-set-mock! [this method val]
                                  (list `swap! state `update :mocks `assoc method val)))
                          (let [[this method] (repeatedly gensym)]
                            (list '-get-mock [this method]
                                  (list `get-in (list `deref state) [:mocks method]))))))]
     `(let [~state (atom {:mocks ~defaults})]
        ~mock))))

(defn mock? [x]
  (satisfies? IMock x))

(defn set-mock! [mock method f-or-val]
  (-set-mock! mock method (if (fn? f-or-val) f-or-val (constantly f-or-val)))
  mock)

(defn remove-mock! [mock method]
  (-set-mock! mock method nil)
  mock)

(defn calls [mock method]
  (get @mock method))

(defn use! [mock method & vals]
  (when (seq vals)
    (let [results (atom vals)
          curr (-get-mock mock method)]
      (-set-mock! mock method (fn [& _]
                                (let [result (first @results)]
                                  (when (empty? (swap! results rest))
                                    (-set-mock! mock method curr))
                                  (cond-> result
                                    (instance? Throwable result) throw))))))
  mock)
