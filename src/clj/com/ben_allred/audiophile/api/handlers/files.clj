(ns com.ben-allred.audiophile.api.handlers.files
  (:require
    [com.ben-allred.audiophile.api.services.interactors.core :as int]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(defmethod ig/init-key ::upload [_ {:keys [interactor]}]
  (fn [data]
    (int/create-artifact! interactor
                          data
                          (select-keys data #{:user/id}))))

(defmethod ig/init-key ::fetch-all [_ {:keys [interactor]}]
  (fn [data]
    (int/query-many interactor data)))

(defmethod ig/init-key ::create [_ {:keys [interactor]}]
  (fn [{project-id :project/id user-id :user/id :as data}]
    (int/create-file! interactor project-id data {:user/id user-id})))

(defmethod ig/init-key ::create-version [_ {:keys [interactor]}]
  (fn [data]
    (int/create-file-version! interactor
                              (:file/id data)
                              data
                              (select-keys data #{:user/id}))))

(defmethod ig/init-key ::download [_ {:keys [interactor]}]
  (fn [_]
    (int/not-implemented!)))
