(ns com.ben-allred.audiophile.common.services.resources.toaster
  (:require
    [com.ben-allred.audiophile.common.services.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.services.ui-store.actions :as actions]
    [com.ben-allred.audiophile.common.services.ui-store.core :as ui-store]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.vow.impl.protocol :as pv])
  #?(:clj
     (:import
       (clojure.lang IDeref))))

(defn ^:private ->toast [store level f linger-ms lag-ms]
  (fn [result]
    (when-let [body (when f (f result))]
      (let [action (if (and linger-ms lag-ms)
                     (actions/toast! level body linger-ms lag-ms)
                     (actions/toast! level body))]
        (ui-store/dispatch! store action)))))

(deftype ToastResource [*resource store success-fn error-fn linger-ms lag-ms]
  pres/IResource
  (request! [_ opts]
    (-> (pres/request! *resource opts)
        (v/peek (->toast store :success success-fn linger-ms lag-ms)
                (->toast store :error error-fn linger-ms lag-ms))))
  (status [_]
    (pres/status *resource))

  pv/IPromise
  (then [_ on-success on-error]
    (v/then *resource on-success on-error))

  IDeref
  (#?(:cljs -deref :default deref) [_]
    @*resource))

(defn resource [{:keys [error-fn resource store success-fn]}]
  (->ToastResource resource store success-fn error-fn nil nil))

(defn toast-fn [{:keys [msg]}]
  (constantly msg))
