(ns com.ben-allred.audiophile.common.utils.core
  (:require
    [com.ben-allred.audiophile.common.utils.logger :as log])
  #?(:cljs
     (:require-macros
       com.ben-allred.audiophile.common.utils.core)))

(defmacro silent! [& body]
  (let [type (if (:ns &env) :default `Throwable)]
    `(try
       ~@body
       (catch ~type _# nil))))
