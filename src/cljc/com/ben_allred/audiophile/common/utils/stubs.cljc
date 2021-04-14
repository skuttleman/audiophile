(ns com.ben-allred.audiophile.common.utils.stubs
  (:refer-clojure :exclude [atom])
  #?(:cljs
     (:require
       [reagent.core :as reagent]
       [reagent.ratom :as ratom]))
  #?(:clj
     (:import
       (clojure.lang IDeref))))

(def atom #?(:cljs    reagent/atom
             :default clojure.core/atom))

(def create-class #?(:cljs    reagent/create-class
                     :default :reagent-render))

(def argv #?(:cljs    reagent/argv
             :default (constantly nil)))

(def adapt-react-class #?(:cljs    reagent/adapt-react-class
                          :default (constantly :div)))

(def make-reaction #?(:cljs    ratom/make-reaction
                      :default #(reify IDeref (deref [_] (%)))))
