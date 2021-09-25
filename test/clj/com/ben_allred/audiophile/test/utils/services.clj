(ns com.ben-allred.audiophile.test.utils.services
  (:require
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.protocols :as pps]
    [com.ben-allred.audiophile.common.infrastructure.pubsub.protocols :as ppubsub]
    [com.ben-allred.audiophile.test.utils.stubs :as stubs]))

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
