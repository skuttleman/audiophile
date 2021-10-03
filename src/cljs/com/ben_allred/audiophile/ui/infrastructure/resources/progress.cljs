(ns com.ben-allred.audiophile.ui.infrastructure.resources.progress
  (:require
    [com.ben-allred.audiophile.common.api.pubsub.core :as pubsub]
    [com.ben-allred.audiophile.common.core.resources.protocols :as pres]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]
    [com.ben-allred.vow.core :as v]
    [com.ben-allred.vow.impl.protocol :as pv]))

(deftype ProgressResource [state pubsub *resource]
  pres/IResource
  (request! [_ opts]
    (if-let [on-progress (:on-progress opts)]
      (let [[progress-id request-id] (repeatedly uuids/random)]
        (swap! state assoc :progress-id progress-id)
        (pubsub/subscribe! pubsub request-id progress-id on-progress)
        (-> opts
            (assoc :progress/id progress-id)
            (->> (pres/request! *resource))
            (v/peek (fn [[status]]
                      (on-progress progress-id {:progress/status status})
                      (pubsub/unsubscribe! pubsub request-id)))))
      (pres/request! *resource opts)))
  (status [_]
    (pres/status *resource))

  pv/IPromise
  (then [_ on-success on-error]
    (v/then *resource on-success on-error))

  IDeref
  (-deref [this]
    (let [{:keys [progress]} @state]
      (case (pres/status this)
        :requesting progress
        @*resource))))

(defn resource [{:keys [*resource pubsub]}]
  (->ProgressResource (atom nil) pubsub *resource))
