(ns com.ben-allred.audiophile.api.handlers.files
  (:require
    [com.ben-allred.audiophile.api.services.repositories.files :as files]
    [com.ben-allred.audiophile.common.utils.http :as http]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(defmethod ig/init-key ::upload [_ {:keys [artifact-repo]}]
  (fn [request]
    (log/spy (:valid/data request))
    (let [artifact-data (get-in request [:params "files[]"])
          user-id (get-in request [:auth/user :data :user :user/id])
          artifact (files/create-artifact artifact-repo artifact-data user-id)]
      [::http/ok {:data artifact}])))

(defmethod ig/init-key ::download [_ _]
  (fn [request]
    [::http/ok {:data {:ya/done :good}}]))
