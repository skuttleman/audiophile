(ns com.ben-allred.audiophile.api.services.repositories.files
  (:require
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.entities.core :as entities]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.utils.uuids :as uuids]))

(defmacro ^:private with-unexceptional [fut & body]
  `(let [future# (future ~fut)]
     (try (let [result# (do ~@body)]
            @future#
            result#)
          (catch Throwable ex#
            (future-cancel future#)
            (throw ex#)))))

(defn create-artifact
  "Save an artifact to the repository and upload the content to the kv store.
   If write to kv store fails, repository will be rolled back. Otherwise, cleanup TBD"
  [repo artifact user-id]
  (repos/transact! repo (fn [executor {store :store/kv entity :entity/artifacts}]
                          (let [key (str "artifacts/" (uuids/random))]
                            (with-unexceptional (repos/put! store
                                                            key
                                                            (:tempfile artifact)
                                                            {:content-type (:content-type artifact)
                                                             :metadata     {:filename (:filename artifact)}})
                              (let [data (-> artifact
                                             (select-keys #{:content-type :filename})
                                             (assoc :uri (repos/uri store key)
                                                    :content-size (:size artifact)
                                                    :created-by user-id))]
                                {:artifact/id (->> data
                                                   (entities/insert-into entity)
                                                   (repos/execute! executor)
                                                   first
                                                   :id)}))))))
