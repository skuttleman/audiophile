(ns audiophile.test.utils.services
  (:require
    [audiophile.backend.api.pubsub.protocols :as pps]
    [audiophile.backend.infrastructure.repositories.protocols :as prepos]
    [audiophile.common.api.pubsub.protocols :as ppubsub]
    [audiophile.test.utils.stubs :as stubs]
    [spigot.controllers.kafka.protocols :as sp.kproto]))

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

(defn ->producer []
  (stubs/create (reify
                  sp.kproto/ISpigotProducer
                  (send! [_ _ _ _]
                    (future nil)))))

(defn ->store []
  (stubs/create (reify
                  prepos/IKVStore
                  (uri [_ key _]
                    (str "test://uri/" key))
                  (get [_ _ _])
                  (put! [_ _ _ _]))))

(defn ->tx []
  (stubs/create (reify
                  prepos/ITransact
                  (transact! [this f]
                    (f this))

                  prepos/IExecute
                  (execute! [_ _ _]))))