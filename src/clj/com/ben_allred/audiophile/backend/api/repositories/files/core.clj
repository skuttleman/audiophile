(ns com.ben-allred.audiophile.backend.api.repositories.files.core
  (:refer-clojure :exclude [accessor])
  (:require
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.files.protocols :as pf]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn ^:private create-artifact* [executor artifact opts]
  (let [artifact-id (pf/insert-artifact! executor artifact opts)]
    {:artifact/id artifact-id
     :artifact/filename (:filename artifact)}))

(defn ^:private create-file* [executor file opts]
  (let [file-id (pf/insert-file! executor file opts)]
    (pf/find-by-file-id executor file-id (assoc opts :internal/verified? true))))

(defn ^:private create-file-version* [executor version opts]
  (pf/insert-version! executor version opts)
  (pf/find-by-file-id executor (:file/id opts) (assoc opts :internal/verified? true)))

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
    (repos/transact! repo create-artifact* opts opts))
  (create-file! [_ opts]
    (repos/transact! repo create-file* opts opts))
  (create-file-version! [_ opts]
    (repos/transact! repo create-file-version* opts opts))
  (get-artifact [_ opts]
    (repos/transact! repo get-artifact* (:artifact/id opts) opts)))

(defn accessor [{:keys [repo]}]
  (->FileAccessor repo))
