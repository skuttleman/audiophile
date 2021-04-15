(ns com.ben-allred.audiophile.common.services.forms.core
  (:require
    [com.ben-allred.audiophile.common.services.forms.protocols :as pforms]
    [com.ben-allred.audiophile.common.services.resources.core :as res]
    [com.ben-allred.audiophile.common.services.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.utils.logger :as log])
  #?(:clj
     (:import
       (clojure.lang IDeref))))

(defn ^:private derefable? [x]
  #?(:cljs    (satisfies? IDeref x)
     :default (instance? IDeref x)))

(defn init! [form value]
  (pforms/init! form value))

(defn update! [form path value]
  (pforms/update! form path value))

(defn visit!
  ([form]
   (pforms/visit! form))
  ([form path]
   (pforms/visit! form path)))

(defn visited?
  ([form]
   (pforms/visited? form))
  ([form path]
   (pforms/visited? form path)))

(defn changed? [form path]
  (pforms/changed? form path))

(defn errors [form]
  (pforms/errors form))

(defn valid? [form]
  (empty? (errors form)))

(defn with-attrs
  ([form path]
   (with-attrs nil form path))
  ([attrs form path]
   (let [visited? (when (satisfies? pforms/ITrack form)
                    (visited? form path))
         errors (when (satisfies? pforms/IValidate form)
                  (get-in (errors form) path))
         status (when (satisfies? pres/IResource form)
                  (pres/status form))]
     (-> attrs
         (assoc :visited? visited?)
         (update :disabled #(or % (= :requesting status)))
         (update :on-blur (fn [on-blur]
                            (fn [e]
                              (visit! form path)
                              (when on-blur
                                (on-blur e)))))
         (cond->
           (derefable? form)
           (assoc :value (get-in @form path))

           (satisfies? pforms/IChange form)
           (assoc :on-change (partial pforms/update! form path))

           (and visited? errors)
           (assoc :errors errors))))))
