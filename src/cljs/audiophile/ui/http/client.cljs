(ns audiophile.ui.http.client
  (:require
    [audiophile.common.api.pubsub.core :as pubsub]
    [audiophile.common.core.utils.uuids :as uuids]
    [audiophile.common.infrastructure.http.impl :as ihttp]
    [audiophile.common.infrastructure.resources.protocols :as pres]
    [com.ben-allred.vow.core :as v :include-macros true]))

(defn ^:private with-async* [http-client pubsub ms {{request-id :x-request-id} :headers :as request}]
  (let [pubsub-id (uuids/random)]
    (-> (v/create (fn [resolve reject]
                    (pubsub/subscribe! pubsub pubsub-id request-id (fn [_ event]
                                                                     (if (:error event)
                                                                       (reject event)
                                                                       (resolve event))))
                    (-> http-client
                        (pres/request! request)
                        (v/catch reject))
                    (v/and (v/sleep ms)
                           (reject {:errors [{:message "timeout waiting for result"}]}))))
        (v/peek (fn [_]
                  (pubsub/unsubscribe! pubsub pubsub-id))))))

(deftype AsyncHttpClient [http-client pubsub timeout]
  pres/IResource
  (request! [_ opts]
    (let [request (-> opts
                      (assoc-in [:headers :x-request-id] (uuids/random))
                      (dissoc :http/async?))]
      (if (:http/async? opts)
        (with-async* http-client pubsub timeout request)
        (pres/request! http-client request)))))

(defn client [{:keys [http-client pubsub timeout]}]
  (->AsyncHttpClient http-client pubsub (or timeout ihttp/default-timeout)))
