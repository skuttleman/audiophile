(ns com.ben-allred.audiophile.ui.infrastructure.resources.toaster
  (:require
    [com.ben-allred.audiophile.common.core.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.maps :as maps]
    [com.ben-allred.audiophile.ui.core.components.core :as comp]
    [com.ben-allred.vow.core :as v]))

(defn ^:private with-msg [msg]
  (fn [result]
    (cond-> result
      (satisfies? IMeta result)
      (vary-meta update :toast/msg #(or % msg)))))

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

  IDeref
  (-deref [_]
    @*resource))

(defn resource [{:keys [error-fn error-msg resource *toasts success-fn success-msg]}]
  (->ToastResource resource
                   *toasts
                   (cond-> success-fn
                     success-msg (comp (with-msg success-msg)))
                   (cond-> error-fn
                     error-msg (comp (with-msg error-msg)))))

(defn toast-fn [{:keys [msg]}]
  (fn [result]
    (or (:toast/msg (meta result)) msg)))
