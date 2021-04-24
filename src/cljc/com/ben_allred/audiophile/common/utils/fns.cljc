(ns com.ben-allred.audiophile.common.utils.fns)

(defn sidecar!
  "takes a sequence of fns and returns a function that calls all of them with the same args, but only returns
   the value of the first fn. Useful for side effectful things."
  ([f] f)
  ([f & fns]
   (comp first (apply juxt (cons f fns)))))
