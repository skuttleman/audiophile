(ns audiophile.test.utils.services
  (:require
    [audiophile.backend.api.pubsub.protocols :as pps]
    [audiophile.backend.api.repositories.protocols :as prepos]
    [audiophile.common.api.pubsub.protocols :as ppubsub]
    [audiophile.test.utils.stubs :as stubs]))

(defn ->pubsub []
  (stubs/create (reify
                  ppubsub/IPub
                  (publish! [_ _ _])
                  ppubsub/ISub
                  (subscribe! [_ _ _ _])
                  (unsubscribe! [_ _])
                  (unsubscribe! [_ _ _]))))

(defn ->chan []
  (stubs/create (reify
                  pps/IChannel
                  (open? [_])
                  (send! [_ _])
                  (close! [_]))))

(defn ->store []
  (stubs/create (reify
                  prepos/IKVStore
                  (uri [_ _ _])
                  (get [_ _ _])
                  (put! [_ _ _ _]))))
