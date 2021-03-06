(ns audiophile.backend.infrastructure.stores
  (:require
    [audiophile.backend.infrastructure.repositories.protocols :as prepos]))

(deftype ArtifactStore [store max-file-size]
  prepos/IKVStore
  (uri [_ key opts]
    (prepos/uri store key opts))
  (get [_ key opts]
    (prepos/get store key opts))
  (put! [_ key {:artifact/keys [content-type filename size tempfile]} opts]
    (prepos/put! store key tempfile (assoc opts
                                           :content-type content-type
                                           :content-length size
                                           :metadata {:filename filename}))))

(defn artifact-store [{:keys [max-file-size store]}]
  (->ArtifactStore store max-file-size))
