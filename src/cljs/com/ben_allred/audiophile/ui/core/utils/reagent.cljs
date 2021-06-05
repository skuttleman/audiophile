(ns com.ben-allred.audiophile.ui.core.utils.reagent
  (:refer-clojure :exclude [atom])
  (:require
    [reagent.core :as reagent]
    [reagent.ratom :as ratom]))

(def atom reagent/atom)

(def create-class reagent/create-class)

(def argv reagent/argv)

(def adapt-react-class reagent/adapt-react-class)

(def make-reaction ratom/make-reaction)
