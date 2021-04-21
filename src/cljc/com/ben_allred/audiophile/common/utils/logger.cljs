(ns com.ben-allred.audiophile.common.utils.logger
  (:require-macros
    [com.ben-allred.audiophile.common.utils.logger])
  (:require
    [clojure.pprint :as pp]
    clojure.string
    taoensso.timbre))

(def ^:dynamic *ctx* nil)

(defn pprint [value]
  "reagent component for displaying clojure data in the browser - debug only"
  [:pre (with-out-str (pp/pprint value))])
