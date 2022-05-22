(ns audiophile.ui.resources.impl
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.common.infrastructure.resources.protocols :as pres]
    [com.ben-allred.vow.core :as v]
    [reagent.core :as r]))

(deftype ReactiveResource [state opts->vow]
  pres/IResource
  (request! [_ opts]
    (swap! state assoc :status :requesting)
    (-> (opts->vow opts)
        (v/peek (partial swap! state assoc :status :success :value)
                (partial swap! state assoc :status :error :errors))))
  (status [_]
    (:status @state))

  IDeref
  (-deref [_]
    (let [{:keys [status value errors]} @state]
      (case status
        :error errors
        value))))

(defn base [opts->vow]
  (->ReactiveResource (r/atom {:status :init}) opts->vow))

(defn http
  ([http-client opts->req]
   (http http-client opts->req identity))
  ([http-client opts->req handler]
   (base (fn [opts]
           (-> (res/request! http-client (opts->req opts))
               (v/then :data (comp v/reject :error))
               handler)))))