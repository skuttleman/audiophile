(ns audiophile.backend.api.repositories.files.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [audiophile.backend.api.repositories.core :as repos]
    [audiophile.backend.api.repositories.files.core :as rfiles]
    [audiophile.backend.domain.interactors.protocols :as pint]
    [audiophile.backend.api.pubsub.core :as ps]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.uuids :as uuids]))

(defn ^:private get-artifact* [executor store artifact-id opts]
  (when-let [{:artifact/keys [content-type key]} (rfiles/find-by-artifact-id executor artifact-id opts)]
    (if-let [data (some->> key (repos/get store))]
      [data {:content-type content-type}]
      (throw (ex-info "artifact located with missing data" {:artifact-id artifact-id})))))

(deftype FileAccessor [repo store ch pubsub keygen publish-threshold]
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo rfiles/select-for-project (:project/id opts) opts))
  (query-one [_ opts]
    (repos/transact! repo rfiles/find-by-file-id (:file/id opts) (assoc opts :includes/versions? true)))

  pint/IFileAccessor
  (create-artifact! [_ data opts]
    (let [key (keygen)
          uri (repos/uri store key opts)
          data (assoc data :artifact/uri uri :artifact/key key)
          payload (dissoc data :artifact/tempfile)]
      (repos/put! store key data opts)
      (ps/emit-command! ch :artifact/create! payload opts)))
  (create-file! [_ data opts]
    (ps/emit-command! ch :file/create! data opts))
  (create-file-version! [_ data opts]
    (ps/emit-command! ch :file-version/create! data opts))
  (get-artifact [_ opts]
    (repos/transact! repo get-artifact* store (:artifact/id opts) opts)))

(defn accessor
  "Constructor for [[FileAccessor]] which provides semantic access for storing and retrieving files."
  [{:keys [ch pubsub publish-threshold repo store]}]
  (->FileAccessor repo
                  store
                  ch
                  pubsub
                  #(str "artifacts/" (uuids/random))
                  (or publish-threshold 150000)))
