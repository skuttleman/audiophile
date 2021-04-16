(ns com.ben-allred.audiophile.common.services.resources.toaster
  (:require
    [com.ben-allred.audiophile.common.services.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.services.ui-store.actions :as actions]
    [com.ben-allred.audiophile.common.services.ui-store.core :as ui-store]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.vow.impl.protocol :as pv]
    [integrant.core :as ig])
  #?(:clj
     (:import
       (clojure.lang IDeref))))

(defn ^:private ->toast [store level f]
  (fn [x]
    (when-let [body (when f (f x))]
      (ui-store/dispatch! store (actions/toast! level body)))))

(deftype ToastResource [resource store success-fn error-fn]
  pres/IResource
  (request! [_ opts]
    (-> (pres/request! resource opts)
        (v/peek (->toast store :success success-fn)
                (->toast store :error error-fn))))
  (status [_]
    (pres/status resource))

  pv/IPromise
  (then [_ on-success on-error]
    (v/then resource on-success on-error))

  IDeref
  (#?(:cljs -deref :default deref) [_]
    @resource))

(defmethod ig/init-key ::resource [_ {:keys [error-fn resource store success-fn]}]
  (->ToastResource resource store success-fn error-fn))

(defmethod ig/init-key ::result-fn [_ {:keys [msg]}]
  (constantly msg))
