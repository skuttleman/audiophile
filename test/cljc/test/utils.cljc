(ns test.utils
  (:require
    [clojure.core.async :as async]
    [com.ben-allred.vow.core :as v]))

(defmacro async [cb ch]
  `(let [~cb (constantly nil)]
     (async/<!! ~ch)))

(defn prom->ch [prom]
  (let [ch (async/promise-chan)]
    (v/peek prom #(async/go (async/>! ch %)))
    ch))
