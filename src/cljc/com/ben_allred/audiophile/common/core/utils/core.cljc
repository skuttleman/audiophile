(ns com.ben-allred.audiophile.common.core.utils.core
  #?(:cljs
     (:require-macros
       com.ben-allred.audiophile.common.core.utils.core))
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defmacro silent! [& body]
  (let [type (if (:ns &env) :default `Throwable)]
    `(try
       ~@body
       (catch ~type _# nil))))
