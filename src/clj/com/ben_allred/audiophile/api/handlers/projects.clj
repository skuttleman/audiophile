(ns com.ben-allred.audiophile.api.handlers.projects
  (:require
    [com.ben-allred.audiophile.api.services.interactors.core :as int]
    [com.ben-allred.audiophile.common.utils.logger :as log]
    [integrant.core :as ig]))

(defmethod ig/init-key ::fetch-all [_ {:keys [interactor]}]
  (fn [data]
    (int/query-many interactor data)))

(defmethod ig/init-key ::fetch [_ {:keys [interactor]}]
  (fn [data]
    (int/query-one interactor data)))

(defmethod ig/init-key ::create [_ {:keys [interactor]}]
  (fn [project]
    (int/create! interactor
                 project
                 (select-keys project #{:user/id}))))
