(ns com.ben-allred.audiophile.ui.infrastructure.resources.toaster
  (:require
    [com.ben-allred.audiophile.common.core.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.audiophile.ui.core.components.core :as comp]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.vow.impl.protocol :as pv]))

(defn ^:private ->toast [*toasts level f]
  (fn [result]
    (when-let [body (when f (f result))]
      (comp/create! *toasts (maps/->m level body)))))

(deftype ToastResource [*resource *toasts success-fn error-fn]
  pres/IResource
  (request! [_ opts]
    (-> *resource
        (pres/request! opts)
        (v/peek (->toast *toasts :success success-fn)
                (->toast *toasts :error error-fn))))
  (status [_]
    (pres/status *resource))

  pv/IPromise
  (then [_ on-success on-error]
    (v/then *resource on-success on-error))

  IDeref
  (-deref [_]
    @*resource))

(defn resource [{:keys [error-fn resource *toasts success-fn]}]
  (->ToastResource resource *toasts success-fn error-fn))

(defn toast-fn [{:keys [msg]}]
  (fn [result]
    (or (:toast/msg (meta result)) msg)))
