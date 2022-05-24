(ns audiophile.common.core.utils.keywords
  (:refer-clojure :exclude [str]))

(defn str
  "stringify a keyword in a way that survives a round trip back to the same keyword
   when passed to [[clojure.core/keyword]]."
  [k]
  (clojure.core/str (when-let [ns (namespace k)]
                      (clojure.core/str ns "/"))
                    (name k)))
