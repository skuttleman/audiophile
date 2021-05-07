(ns com.ben-allred.audiophile.common.utils.strings
  (:refer-clojure :exclude [format])
  (:require
    #?@(:cljs [[goog.string :as gstring]
               [goog.string.format]])
    [clojure.string :as string]))

(def ^{:arglists '([fmt & args])} format
  #?(:cljs gstring/format
     :default clojure.core/format))
