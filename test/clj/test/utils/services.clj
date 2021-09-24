(ns test.utils.services
  (:require
    [com.ben-allred.audiophile.common.infrastructure.pubsub.protocols :as ppubsub]
    [test.utils.stubs :as stubs]))

(defn ->pubsub []
  (stubs/create (reify
                  ppubsub/IPub
                  (publish! [_ _ _])
                  ppubsub/ISub
                  (subscribe! [_ _ _ _])
                  (unsubscribe! [_ _])
                  (unsubscribe! [_ _ _]))))
