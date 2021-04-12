(ns com.ben-allred.audiophile.common.utils.macros)

(defmacro ignore! [& body]
  `(try ~@body (catch Throwable _# nil)))
