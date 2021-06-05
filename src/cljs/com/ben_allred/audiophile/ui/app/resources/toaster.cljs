(ns com.ben-allred.audiophile.ui.app.resources.toaster
  (:require
    [com.ben-allred.audiophile.common.core.resources.core :as res]
    [com.ben-allred.audiophile.common.core.resources.protocols :as pres]
    [com.ben-allred.audiophile.ui.core.components.core :as comp]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.vow.impl.protocol :as pv]))

(defn ^:private ->toast [*toasts level f]
  (fn [result]
    (when-let [body (when f (f result))]
      (comp/create! *toasts level body))))

(deftype ToastResource [*resource *toasts success-fn error-fn]
  pres/IResource
  (request! [_ opts]
    (-> (res/request! *resource opts)
        (v/peek (->toast *toasts :success success-fn)
                (->toast *toasts :error error-fn))))
  (status [_]
    (res/status *resource))

  pv/IPromise
  (then [_ on-success on-error]
    (v/then *resource on-success on-error))

  IDeref
  (-deref [_]
    @*resource))

(defn resource [{:keys [error-fn resource *toasts success-fn]}]
  (->ToastResource resource *toasts success-fn error-fn))

(defn toast-fn [{:keys [msg]}]
  (constantly msg))
