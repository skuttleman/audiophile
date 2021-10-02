(ns com.ben-allred.audiophile.test.utils.services
  (:require
    [com.ben-allred.audiophile.backend.api.pubsub.protocols :as pps]
    [com.ben-allred.audiophile.backend.api.repositories.files.protocols :as pf]
    [com.ben-allred.audiophile.backend.api.repositories.protocols :as prepos]
    [com.ben-allred.audiophile.common.api.pubsub.protocols :as ppubsub]
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

(defn ->store []
  (stubs/create (reify
                  pf/IArtifactStore
                  (supported? [_ _ _])
                  prepos/IKVStore
                  (uri [_ _ _])
                  (get [_ _ _])
                  (put! [_ _ _ _]))))
