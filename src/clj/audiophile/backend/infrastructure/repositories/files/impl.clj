(ns audiophile.backend.infrastructure.repositories.files.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.backend.infrastructure.repositories.files.queries :as q]
    [audiophile.backend.domain.interactors.protocols :as pint]
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.uuids :as uuids]))

(defn ^:private get-artifact* [executor store artifact-id opts]
  (when-let [{:artifact/keys [content-type key]} (q/find-by-artifact-id executor artifact-id opts)]
    (if-let [data (some->> key (repos/get store))]
      [data {:content-type content-type}]
      (throw (ex-info "artifact located with missing data" {:artifact-id artifact-id})))))

(defn ^:private file-accessor#create-artifact! [store ch key data opts]
  (let [uri (repos/uri store key opts)
        data (assoc data :artifact/uri uri :artifact/key key)
        payload (dissoc data :artifact/tempfile)]
    (repos/put! store key data opts)
    (ps/start-workflow! ch :artifacts/create payload opts)))

(deftype FileAccessor [repo store ch pubsub keygen]
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo q/select-for-project (:project/id opts) opts))
  (query-one [_ opts]
    (repos/transact! repo q/find-by-file-id (:file/id opts) (assoc opts :includes/versions? true)))

  pint/IFileAccessor
  (create-artifact! [_ data opts]
    (file-accessor#create-artifact! store ch (keygen) data opts))
  (create-file! [_ data opts]
    (ps/start-workflow! ch :files/create (merge opts data) opts))
  (create-file-version! [_ data opts]
    (ps/start-workflow! ch :versions/create (merge opts data) opts))
  (get-artifact [_ opts]
    (repos/transact! repo get-artifact* store (:artifact/id opts) opts)))

(defn accessor
  "Constructor for [[FileAccessor]] which provides semantic access for storing and retrieving files."
  [{:keys [ch pubsub repo store]}]
  (->FileAccessor repo store ch pubsub #(str "artifacts/" (uuids/random))))
