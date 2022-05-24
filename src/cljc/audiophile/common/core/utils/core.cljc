(ns audiophile.common.core.utils.core
  #?(:cljs
     (:require-macros
       audiophile.common.core.utils.core))
  (:require
    [audiophile.common.core.utils.logger :as log]))

(defmacro silent! [& body]
  (let [type (if (:ns &env) :default `Throwable)]
    `(try
       ~@body
       (catch ~type _# nil))))
