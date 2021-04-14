(ns com.ben-allred.audiophile.common.utils.logger
  (:require-macros
    [com.ben-allred.audiophile.common.utils.logger])
  (:require
    [taoensso.timbre]
    [clojure.pprint :as pp]))

(defn pprint [value]
  [:pre (with-out-str (pp/pprint value))])
