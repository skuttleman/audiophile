(ns com.ben-allred.audiophile.common.utils.keywords
  (:refer-clojure :exclude [str]))

(defn str [k]
  (clojure.core/str (when-let [ns (namespace k)]
                      (clojure.core/str ns "/"))
                    (name k)))
