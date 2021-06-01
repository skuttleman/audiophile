(ns com.ben-allred.audiophile.common.app.resources.toaster
  (:require
    [com.ben-allred.audiophile.common.app.resources.core :as res]
    [com.ben-allred.audiophile.common.app.resources.protocols :as pres]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.vow.impl.protocol :as pv])
  #?(:clj
     (:import
       (clojure.lang IDeref))))

(defn ^:private ->toast [toaster level f]
  (fn [result]
    (when-let [body (when f (f result))]
      (res/toast! toaster level body))))

(deftype ToastResource [*resource toaster success-fn error-fn]
  pres/IResource
  (request! [_ opts]
    (-> (pres/request! *resource opts)
        (v/peek (->toast toaster :success success-fn)
                (->toast toaster :error error-fn))))
  (status [_]
    (pres/status *resource))

  pv/IPromise
  (then [_ on-success on-error]
    (v/then *resource on-success on-error))

  IDeref
  (#?(:cljs -deref :default deref) [_]
    @*resource))

(defn resource [{:keys [error-fn resource toaster success-fn]}]
  (->ToastResource resource toaster success-fn error-fn))

(defn toast-fn [{:keys [msg]}]
  (constantly msg))
