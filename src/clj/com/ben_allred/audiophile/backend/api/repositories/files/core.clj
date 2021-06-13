(ns com.ben-allred.audiophile.backend.api.repositories.files.core
  (:refer-clojure :exclude [accessor])
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.common :as crepos]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.files.protocols :as pf]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn ^:private create-artifact* [executor artifact opts]
  (let [artifact-id (pf/insert-artifact! executor artifact opts)
        artifact (pf/find-event-artifact executor artifact-id)]
    (pf/artifact-created! executor (:user/id opts) artifact opts)))

(defn ^:private create-file* [executor file opts]
  (let [file-id (pf/insert-file! executor file opts)
        file (pf/find-event-file executor file-id)]
    (pf/file-created! executor (:user/id opts) file opts)))

(defn ^:private create-file-version* [executor version opts]
  (let [version-id (pf/insert-version! executor version opts)
        version (pf/find-event-version executor version-id)]
    (pf/version-created! executor (:user/id opts) version opts)))

(defn ^:private get-artifact* [executor artifact-id opts]
  (let [{:artifact/keys [data content-type]} (pf/find-by-artifact-id executor artifact-id opts)]
    [data {:content-type content-type}]))

(deftype FileAccessor [repo]
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo pf/select-for-project (:project/id opts) opts))
  (query-one [_ opts]
    (repos/transact! repo pf/find-by-file-id (:file/id opts) (assoc opts :includes/versions? true)))

  pint/IFileAccessor
  (create-artifact! [_ opts]
    (crepos/command! repo opts
      (repos/transact! repo create-artifact* opts opts)))
  (create-file! [_ opts]
    (crepos/command! repo opts
      (repos/transact! repo create-file* opts opts)))
  (create-file-version! [_ opts]
    (crepos/command! repo opts
      (repos/transact! repo create-file-version* opts opts)))
  (get-artifact [_ opts]
    (repos/transact! repo get-artifact* (:artifact/id opts) opts)))

(defn accessor [{:keys [repo]}]
  (->FileAccessor repo))
