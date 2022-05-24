(ns audiophile.test.utils
  #?(:cljs
     (:require-macros
       audiophile.test.utils))
  (:require
    #?(:clj [ring.middleware.cookies :as ring.cook])
    [clojure.core.async :as async]
    [com.ben-allred.vow.core :as v]))

(defmacro async [cb body]
  (if (:ns &env)
    `(clojure.test/async ~cb ~body)
    (list* `do (butlast (rest body)))))

(defn prom->ch [prom]
  (let [ch (async/promise-chan)]
    (v/peek prom #(async/go (async/>! ch %)))
    ch))

(defn ^:private <!* [ch ms]
  (async/go
    (let [[val] (async/alts! [ch (async/go
                                   (async/<! (async/timeout ms))
                                   ::timed-out)])]
      (when (= ::timed-out val)
        (async/close! ch))
      val)))

(defn <!ms
  ([ch]
   (<!ms ch 1000))
  ([ch ms]
   (async/go
     (let [val (async/<! (<!* ch ms))]
       (when-not (= ::timed-out val)
         val)))))

#?(:clj
   (defn <!!ms
     ([ch]
      (<!!ms ch 1000))
     ([ch ms]
      (let [val (async/<!! (<!* ch ms))]
        (when (= ::timed-out val)
          (throw (ex-info "timeout expired waiting for a value on chan"
                          {:timeout ms
                           :ch      ch})))
        val))))

(defmacro <p! [prom]
  (if (:ns &env)
    `(async/<! (<!ms (prom->ch ~prom)))
    `(<!!ms (prom->ch ~prom))))

(defmacro <ch! [ch]
  (if (:ns &env)
    `(async/<! (<!ms ~ch))
    `(<!!ms ~ch)))

(defn op-set [[op & args]]
  [op (set args)])

#?(:clj
   (defn decode-cookies
     "Test utility for decoding response cookies"
     [response]
     (->> (get-in response [:headers "Set-Cookie"])
          (map #(:cookies (ring.cook/cookies-request {:headers {"cookie" %}})))
          (reduce merge {}))))
