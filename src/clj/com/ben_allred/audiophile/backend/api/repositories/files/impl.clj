(ns com.ben-allred.audiophile.backend.api.repositories.files.impl
  (:refer-clojure :exclude [accessor])
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.files.core :as rfiles]
    [com.ben-allred.audiophile.backend.domain.interactors.protocols :as pint]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.core :as ps]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.core.utils.uuids :as uuids]))

(defn ^:private create-artifact* [executor artifact opts]
  (if (rfiles/insert-artifact-access? executor artifact opts)
    (let [artifact-id (rfiles/insert-artifact! executor artifact opts)]
      (rfiles/find-event-artifact executor artifact-id))
    (throw (ex-info "insufficient access" {}))))

(defn ^:private create-file* [executor file opts]
  (if (rfiles/insert-file-access? executor file opts)
    (let [file-id (rfiles/insert-file! executor file opts)]
      (rfiles/find-event-file executor file-id))
    (throw (ex-info "insufficient access" {}))))

(defn ^:private create-file-version* [executor file-version opts]
  (if (rfiles/insert-version-access? executor file-version opts)
    (let [version-id (rfiles/insert-version! executor file-version opts)]
      (rfiles/find-event-version executor version-id))
    (throw (ex-info "insufficient access" {}))))

(defn ^:private get-artifact* [executor store artifact-id opts]
  (let [{:artifact/keys [content-type key]} (rfiles/find-by-artifact-id executor artifact-id opts)
        data (repos/get store key)]
    [data {:content-type content-type}]))

(deftype FileAccessor [repo store commands events keygen]
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
      (ps/emit-command! commands (:user/id opts) :artifact/create! (dissoc data :tempfile) opts)))
  (create-file! [_ data opts]
    (ps/emit-command! commands (:user/id opts) :file/create! data opts))
  (create-file-version! [_ data opts]
    (ps/emit-command! commands (:user/id opts) :file-version/create! data opts))
  (get-artifact [_ opts]
    (repos/transact! repo get-artifact* store (:artifact/id opts) opts))

  pint/IMessageHandler
  (handle! [_ {[command-id {:command/keys [type] :as command} ctx] :msg :as msg}]
    (when-let [[data-type id create*]
               (case type
                 :artifact/create! ["artifact" :artifact/id create-artifact*]
                 :file/create! ["file" :file/id create-file*]
                 :file-version/create! ["file-version" :file-version/id create-file-version*]
                 nil)]
      (try
        (log/info "saving" data-type "to db" command-id)
        (let [data (repos/transact! repo create* (:command/data command) ctx)]
          (ps/emit-event! events (:user/id ctx) (get data id) (keyword data-type "created") data ctx))
        (catch Throwable ex
          (log/error ex "failed: saving" data-type "to db" msg)
          (try
            (ps/command-failed! events
                                (:request/id ctx)
                                (assoc ctx
                                       :error/command type
                                       :error/reason (.getMessage ex)))
            (catch Throwable ex
              (log/error ex "failed to emit command/failed"))))))))

(defn accessor
  "Constructor for [[FileAccessor]] which provides semantic access for storing and retrieving files."
  [{:keys [commands events repo store]}]
  (->FileAccessor repo store commands events #(str "artifacts/" (uuids/random))))
