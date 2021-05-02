(ns com.ben-allred.audiophile.api.services.repositories.files
  (:require
    [clojure.java.io :as io]
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.entities.core :as entities]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [com.ben-allred.audiophile.common.utils.uuids :as uuids]))

(defn create-artifact [repo artifact user-id]
  (repos/transact! repo (fn [executor {store :store/kv entity :entity/artifacts}]
                          (let [key (str "artifacts/" (uuids/random))
                                s3-result (future
                                            (repos/put! store
                                                        key
                                                        (io/input-stream (:tempfile artifact))
                                                        {:content-type (:content-type artifact)
                                                         :metadata     {:filename (:filename artifact)
                                                                        :size     (:size artifact)}}))
                                data {:uri        (repos/uri store key)
                                      :created-by user-id}]
                            (try
                              (let [artifact-id (-> executor
                                                    (repos/execute! (entities/insert-into entity data))
                                                    first
                                                    :id)]
                                @s3-result
                                {:artifact/id artifact-id})
                              (catch Throwable ex
                                (future-cancel s3-result)
                                (throw ex)))))))
