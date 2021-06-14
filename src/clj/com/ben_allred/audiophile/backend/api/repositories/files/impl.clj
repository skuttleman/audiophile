(ns com.ben-allred.audiophile.backend.api.repositories.files.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.common :as crepos]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.files.core :as rfiles]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn ^:private create-artifact* [executor artifact opts]
  (let [artifact-id (rfiles/insert-artifact! executor artifact opts)
        artifact (rfiles/find-event-artifact executor artifact-id)]
    (rfiles/artifact-created! executor (:user/id opts) artifact opts)))

(defn ^:private create-file* [executor file opts]
  (let [file-id (rfiles/insert-file! executor file opts)
        file (rfiles/find-event-file executor file-id)]
    (rfiles/file-created! executor (:user/id opts) file opts)))

(defn ^:private create-file-version* [executor version opts]
  (let [version-id (rfiles/insert-version! executor version opts)
        version (rfiles/find-event-version executor version-id)]
    (rfiles/version-created! executor (:user/id opts) version opts)))

(defn ^:private get-artifact* [executor artifact-id opts]
  (let [{:artifact/keys [data content-type]} (rfiles/find-by-artifact-id executor artifact-id opts)]
    [data {:content-type content-type}]))

(deftype FileAccessor [repo]
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo rfiles/select-for-project (:project/id opts) opts))
  (query-one [_ opts]
    (repos/transact! repo rfiles/find-by-file-id (:file/id opts) (assoc opts :includes/versions? true)))
  (create! [_ data opts]
    (crepos/command! repo opts
      (repos/transact! repo create-artifact* data opts)))

  pint/IFileAccessor
  (create-file! [_ data opts]
    (crepos/command! repo opts
      (repos/transact! repo create-file* data opts)))
  (create-file-version! [_ data opts]
    (crepos/command! repo opts
      (repos/transact! repo create-file-version* data opts)))
  (get-artifact [_ opts]
    (repos/transact! repo get-artifact* (:artifact/id opts) opts)))

(defn accessor
  "Constructor for [[FileAccessor]] which provides semantic access for storing and retrieving files."
  [{:keys [repo]}]
  (->FileAccessor repo))
