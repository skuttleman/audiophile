(ns com.ben-allred.audiophile.integration.common.mocks
  (:import
    (clojure.lang IAtom)))

(defprotocol IMock
  (init! [this] "set all mock behavior to initial behavior"))

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
   (let [state (gensym)
         mock (-> (map (fn [x]
                         (if (list? x)
                           (let [[method args & body] x
                                 args' (mapv (fn [_] (gensym)) args)
                                 bindings (into [] (mapcat vector args args'))
                                 f-args (rest args')
                                 k (keyword method)]
                             `(~method ~args'
                                (let [state# @~state]
                                  (if-let [f# (or (get state# ~[k (dec (count args))])
                                                  (get state# ~k))]
                                    (f# ~@f-args)
                                    (let ~bindings ~@body)))))
                           x))
                       reify)
                  (concat
                    (list `IAtom
                          (->method 'swap `swap! state 2)
                          (->method 'swap `swap! state 3)
                          (->method 'swap `swap! state 4)
                          (let [[method args call ret] (->method 'swap `swap! state 5)]
                            (list method args
                                  (list* `apply call)
                                  ret))
                          (->method 'compareAndSet `compare-and-set! state 3)
                          (->method 'reset `reset! state 2)
                          `IMock
                          (let [sym (gensym)]
                            (list 'init! [sym]
                                  (list `reset! state defaults)
                                  sym)))))]
     `(let [~state (atom ~defaults)]
        ~mock))))

(defn mock? [x]
  (satisfies? IMock x))

(defn set-mock!
  ([mock method arity f-or-val]
   (set-mock! mock [method arity] f-or-val))
  ([mock method f-or-val]
   (swap! mock assoc method (if (fn? f-or-val) f-or-val (constantly f-or-val)))))
