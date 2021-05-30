(ns com.ben-allred.audiophile.api.services.repositories.files.core
  (:refer-clojure :exclude [accessor])
  (:require
    [com.ben-allred.audiophile.api.services.interactors.protocols :as pint]
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.files.queries :as q]
    [com.ben-allred.audiophile.common.utils.logger :as log]))

(defn ^:private create-artifact* [executor artifact opts]
  (let [artifact-id (q/insert-artifact! executor artifact opts)]
    {:artifact/id artifact-id
     :artifact/filename (:filename artifact)}))

(defn ^:private create-file* [executor file opts]
  (let [file-id (q/insert-file! executor file opts)]
    (q/find-by-file-id executor file-id (assoc opts :internal/verified? true))))

(defn ^:private create-file-version* [executor version opts]
  (q/insert-version! executor version opts)
  (q/find-by-file-id executor (:file/id opts) (assoc opts :internal/verified? true)))

(defn ^:private get-artifact* [executor artifact-id opts]
  (let [{:artifact/keys [data content-type]} (q/find-by-artifact-id executor artifact-id opts)]
    [data {:content-type content-type}]))

(deftype FileAccessor [repo]
  pint/IAccessor
  (query-many [_ opts]
    (repos/transact! repo q/select-for-project (:project/id opts) opts))
  (query-one [_ opts]
    (repos/transact! repo q/find-by-file-id (:file/id opts) (assoc opts :includes/versions? true)))

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
