(ns com.ben-allred.audiophile.api.handlers.files
  (:require
    [com.ben-allred.audiophile.api.services.repositories.files :as files]
    [com.ben-allred.audiophile.common.utils.colls :as colls]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(defmethod ig/init-key ::upload [_ {:keys [artifact-repo]}]
  (fn [request]
    (let [artifact-data (colls/only! (:valid/data request))
          user-id (get-in request [:auth/user :data :user :user/id])
          artifact (files/create-artifact artifact-repo artifact-data user-id)]
      [::http/ok {:data artifact}])))

(defmethod ig/init-key ::fetch-all [_ {:keys [artifact-repo]}]
  (constantly [::http/not-implemented]))

(defmethod ig/init-key ::create [_ {:keys [artifact-repo]}]
  (constantly [::http/not-implemented]))

(defmethod ig/init-key ::download [_ {:keys [s3-client]}]
  (constantly [::http/not-implemented]))
