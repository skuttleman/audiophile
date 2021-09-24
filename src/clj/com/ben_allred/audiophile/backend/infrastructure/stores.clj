(ns com.ben-allred.audiophile.backend.infrastructure.stores
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.files.protocols :as pf]
    [com.ben-allred.audiophile.backend.api.repositories.protocols :as prepos]))

(deftype ArtifactStore [store]
  pf/IArtifactStore
  prepos/IKVStore
  (uri [_ key opts]
    (prepos/uri store key opts))
  (get [_ key opts]
    (prepos/get store key opts))
  (put! [_ key artifact _]
    (prepos/put! store key (:tempfile artifact) {:content-type   (:content-type artifact)
                                                 :content-length (:size artifact)
                                                 :metadata       {:filename (:filename artifact)}})))

(defn artifact-store [{:keys [store]}]
  (->ArtifactStore store))
