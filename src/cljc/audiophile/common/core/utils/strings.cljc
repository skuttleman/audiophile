(ns audiophile.common.core.utils.strings
  (:refer-clojure :exclude [format])
  (:require
    #?@(:cljs [[goog.string :as gstring]
               [goog.string.format]])
    [clojure.string :as string]))

(def ^{:arglists '([fmt & args])} format
  #?(:cljs    gstring/format
     :default clojure.core/format))

(defn capitalize [s]
  (when s
    (->> (string/split s #"\s+")
         (map (fn [word]
                (str (string/upper-case (first word))
                     (subs word 1))))
         (string/join " "))))
