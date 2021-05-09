(ns test.utils.spies
  #?(:clj
     (:import
       (clojure.lang IFn))))

(defprotocol IStub
  (init! [this] "resets the calls and reverts any changes made with -set-stub")
  (-set-stub [this f] "changes the underlying function used for the stub")
  (calls [this] "returns an ordered sequence of calls to the stub"))

(defn ^:private invoke* [state args]
  (swap! state update :calls (fnil conj []) args)
  (apply (:f @state) args))

(defn create
  ([]
   (create nil))
  ([f-or-val]
   (let [init-state {:f (if (fn? f-or-val) f-or-val (constantly f-or-val))}
         state (atom init-state)]
     (reify
       IStub
       (init! [_]
         (reset! state init-state))
       (-set-stub [_ f]
         (swap! state assoc :f f))
       (calls [_]
         (:calls @state))

       IFn
       (#?(:cljs -invoke :default invoke) [_]
         (invoke* state nil))
       (#?(:cljs -invoke :default invoke) [_ a]
         (invoke* state [a]))
       (#?(:cljs -invoke :default invoke) [_ a b]
         (invoke* state [a b]))
       (#?(:cljs -invoke :default invoke) [_ a b c]
         (invoke* state [a b c]))
       (#?(:cljs -invoke :default invoke) [_ a b c d]
         (invoke* state [a b c d]))
       (#?(:cljs -invoke :default invoke) [_ a b c d e]
         (invoke* state [a b c d e]))
       (#?(:cljs -invoke :default invoke) [_ a b c d e f]
         (invoke* state [a b c d e f]))
       (#?(:cljs -invoke :default invoke) [_ a b c d e f g]
         (invoke* state [a b c d e f g]))
       (#?(:cljs -invoke :default invoke) [_ a b c d e f g h]
         (invoke* state [a b c d e f g h]))
       (#?(:cljs -invoke :default invoke) [_ a b c d e f g h i]
         (invoke* state [a b c d e f g h i]))
       (#?(:cljs -invoke :default invoke) [_ a b c d e f g h i j]
         (invoke* state [a b c d e f g h i j]))
       (#?(:cljs -invoke :default invoke) [_ a b c d e f g h i j k]
         (invoke* state [a b c d e f g h i j k]))
       (#?(:cljs -invoke :default invoke) [_ a b c d e f g h i j k l]
         (invoke* state [a b c d e f g h i j k l]))
       (#?(:cljs -invoke :default invoke) [_ a b c d e f g h i j k l m]
         (invoke* state [a b c d e f g h i j k l m]))
       (#?(:cljs -invoke :default invoke) [_ a b c d e f g h i j k l m n]
         (invoke* state [a b c d e f g h i j k l m n]))
       (#?(:cljs -invoke :default invoke) [_ a b c d e f g h i j k l m n o]
         (invoke* state [a b c d e f g h i j k l m n o]))
       (#?(:cljs -invoke :default invoke) [_ a b c d e f g h i j k l m n o p]
         (invoke* state [a b c d e f g h i j k l m n o p]))
       (#?(:cljs -invoke :default invoke) [_ a b c d e f g h i j k l m n o p q]
         (invoke* state [a b c d e f g h i j k l m n o p q]))
       (#?(:cljs -invoke :default invoke) [_ a b c d e f g h i j k l m n o p q r]
         (invoke* state [a b c d e f g h i j k l m n o p q r]))
       (#?(:cljs -invoke :default invoke) [_ a b c d e f g h i j k l m n o p q r s]
         (invoke* state [a b c d e f g h i j k l m n o p q r s]))
       (#?(:cljs -invoke :default invoke) [_ a b c d e f g h i j k l m n o p q r s t]
         (invoke* state [a b c d e f g h i j k l m n o p q r s t]))
       (#?(:cljs -invoke :default invoke) [_ a b c d e f g h i j k l m n o p q r s t rest]
         (invoke* state (concat [a b c d e f g h i j k l m n o p q r s t] rest)))
       #?(:clj
          (applyTo [_ args]
            (invoke* state args)))))))

(defn stub? [x]
  (satisfies? IStub x))

(defn set-stub! [stub f-or-val]
  (-set-stub stub (if (fn? f-or-val) f-or-val (constantly f-or-val))))
