(ns audiophile.ui.resources.impl
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.common.infrastructure.protocols :as pcom]
    [audiophile.common.infrastructure.resources.core :as res]
    [audiophile.common.infrastructure.resources.protocols :as pres]
    [audiophile.common.infrastructure.store.core :as store]
    [audiophile.ui.store.actions :as act]
    [audiophile.ui.store.queries :as q]
    [com.ben-allred.vow.core :as v]))

(deftype ReactiveResource [id store opts->vow]
  pcom/IIdentify
  (id [_]
    id)

  pres/IResource
  (request! [_ opts]
    (store/dispatch! store (act/resource:set id {:status :requesting}))
    (-> (opts->vow opts)
        (v/peek (fn [[status result]]
                  (let [action (act/resource:set id (maps/->m status result))]
                    (store/dispatch! store action))))))
  (status [_]
    (:status (q/res:state store id)))

  pcom/IDestroy
  (destroy! [_]
    (store/dispatch! store (act/resource:remove id)))

  IDeref
  (-deref [_]
    (:result (q/res:state store id))))

(defn base
  ([store opts-vow]
   (base (uuids/random) store opts-vow))
  ([id store opts->vow]
   (store/dispatch! store (act/resource:init id))
   (->ReactiveResource id store opts->vow)))

(defn http
  ([store http-client opts->req]
   (http store http-client opts->req identity))
  ([store http-client opts->req handler]
   (http (uuids/random) store http-client opts->req handler))
  ([id store http-client opts->req handler]
   (base id store (fn [opts]
                    (-> (res/request! http-client (opts->req opts))
                        (v/then :data (comp v/reject :error))
                        handler)))))
