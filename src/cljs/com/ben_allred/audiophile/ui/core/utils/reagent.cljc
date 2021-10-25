(ns com.ben-allred.audiophile.ui.core.utils.reagent
  #?@(:cljs
      [(:require-macros
         com.ben-allred.audiophile.ui.core.utils.reagent)
       (:refer-clojure :exclude [atom])
       (:require
         [reagent.core :as reagent]
         [reagent.ratom :as ratom])]))

#?(:cljs (def atom reagent/atom))

#?(:cljs (def create-class reagent/create-class))

#?(:cljs (def argv reagent/argv))

#?(:cljs (def adapt-react-class reagent/adapt-react-class))

#?(:cljs (def make-reaction ratom/make-reaction))

(defmacro with-let [bindings & body]
  `(reagent.core/with-let ~bindings ~@body))
