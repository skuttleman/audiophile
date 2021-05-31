(ns com.ben-allred.audiophile.common.utils.strings
  (:refer-clojure :exclude [format])
  #?(:cljs
     (:require
       [goog.string :as gstring]
       [goog.string.format])))

(def ^{:arglists '([fmt & args])} format
  #?(:cljs gstring/format
     :default clojure.core/format))
