(ns com.ben-allred.audiophile.backend.api.repositories.files.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.common :as crepos]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.files.core :as rfiles]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]))

(defn ^:private create-artifact* [executor artifact opts]
  (crepos/with-access (rfiles/insert-artifact-access? executor artifact opts)
    (rfiles/insert-artifact! executor artifact opts)))

(defn ^:private on-artifact-created! [executor artifact-id opts]
  (let [artifact (rfiles/find-event-artifact executor artifact-id)]
    (rfiles/artifact-created! executor (:user/id opts) artifact opts)))

(defn ^:private create-file* [executor file opts]
  (crepos/with-access (rfiles/insert-file-access? executor file opts)
    (rfiles/insert-file! executor file opts)))

(defn ^:private on-file-created! [executor file-id opts]
  (let [file (rfiles/find-event-file executor file-id)]
    (rfiles/file-created! executor (:user/id opts) file opts)))

(defn ^:private create-file-version* [executor version opts]
  (crepos/with-access (rfiles/insert-version-access? executor version opts)
    (rfiles/insert-version! executor version opts)))

(defn ^:private on-file-version-created! [executor version-id opts]
  (let [version (rfiles/find-event-version executor version-id)]
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

  pint/IFileAccessor
  (create-artifact! [_ data opts]
    (let [opts (assoc opts
                      :error/command :artifact/create
                      :error/reason "insufficient access to create artifact"
                      :on-success on-artifact-created!)]
      (crepos/command! repo opts create-artifact* data)))
  (create-file! [_ data opts]
    (let [opts (assoc opts
                      :error/command :file/create
                      :error/reason "insufficient access to create file"
                      :on-success on-file-created!)]
      (crepos/command! repo opts create-file* data)))
  (create-file-version! [_ data opts]
    (let [opts (assoc opts
                      :error/command :file-version/create
                      :error/reason "insufficient access to create file-version"
                      :on-success on-file-version-created!)]
      (crepos/command! repo opts create-file-version* data)))
  (get-artifact [_ opts]
    (repos/transact! repo get-artifact* (:artifact/id opts) opts)))

(defn accessor
  "Constructor for [[FileAccessor]] which provides semantic access for storing and retrieving files."
  [{:keys [repo]}]
  (->FileAccessor repo))
