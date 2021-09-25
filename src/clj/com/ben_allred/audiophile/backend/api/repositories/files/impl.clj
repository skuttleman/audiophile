(ns com.ben-allred.audiophile.backend.api.repositories.files.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.files.core :as rfiles]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.core :as ps]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]))

(defn ^:private get-artifact* [executor store artifact-id opts]
  (let [{:artifact/keys [content-type key]} (rfiles/find-by-artifact-id executor artifact-id opts)
        data (repos/get store key)]
    [data {:content-type content-type}]))

(deftype FileAccessor [repo store pubsub keygen]
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo rfiles/select-for-project (:project/id opts) opts))
  (query-one [_ opts]
    (repos/transact! repo rfiles/find-by-file-id (:file/id opts) (assoc opts :includes/versions? true)))

  pint/IFileAccessor
  (create-artifact! [_ data opts]
    (let [key (keygen)
          uri (repos/uri store key opts)
          data (assoc data :uri uri :key key)]
      (repos/put! store key data)
      (ps/emit-command! pubsub (:user/id opts) :artifact/create! (dissoc data :tempfile) opts)))
  (create-file! [_ data opts]
    (ps/emit-command! pubsub (:user/id opts) :file/create! data opts))
  (create-file-version! [_ data opts]
    (ps/emit-command! pubsub (:user/id opts) :file-version/create! data opts))
  (get-artifact [_ opts]
    (repos/transact! repo get-artifact* store (:artifact/id opts) opts)))

(defn accessor
  "Constructor for [[FileAccessor]] which provides semantic access for storing and retrieving files."
  [{:keys [pubsub repo store]}]
  (->FileAccessor repo store pubsub #(str "artifacts/" (uuids/random))))
