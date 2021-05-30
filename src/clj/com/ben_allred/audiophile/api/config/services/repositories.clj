(ns com.ben-allred.audiophile.api.config.services.repositories
  (:require
    [com.ben-allred.audiophile.api.services.repositories.common :as crepos]
    [com.ben-allred.audiophile.api.services.repositories.core :as repos]
    [com.ben-allred.audiophile.api.services.repositories.files.core :as files]
    [com.ben-allred.audiophile.api.services.repositories.files.queries :as qfiles]
    [com.ben-allred.audiophile.api.services.repositories.models.core :as models]
    [com.ben-allred.audiophile.api.services.repositories.projects.core :as projects]
    [com.ben-allred.audiophile.api.services.repositories.projects.queries :as qprojects]
    [com.ben-allred.audiophile.api.services.repositories.teams.core :as teams]
    [com.ben-allred.audiophile.api.services.repositories.teams.queries :as qteams]
    [com.ben-allred.audiophile.api.services.repositories.users.core :as users]
    [com.ben-allred.audiophile.api.services.repositories.users.queries :as qusers]
    [integrant.core :as ig]))

(defmethod ig/init-key :audiophile.repositories/models [_ cfg]
  (models/models cfg))

(defmethod ig/init-key :audiophile.repositories/model [_ cfg]
  (models/model cfg))

(defmethod ig/init-key :audiophile.repositories/repo [_ cfg]
  (crepos/repo cfg))

(defmethod ig/init-key :audiophile.repositories/store [_ cfg]
  (crepos/store cfg))

(defmethod ig/init-key :audiophile.repositories/query-formatter [_ cfg]
  (repos/query-formatter cfg))

(defmethod ig/init-key :audiophile.repositories/raw-formatter [_ cfg]
  (repos/raw-formatter cfg))

(defmethod ig/init-key :audiophile.repositories/->builder-fn [_ cfg]
  (repos/->builder-fn cfg))

(defmethod ig/init-key :audiophile.repositories/->executor [_ cfg]
  (repos/->executor cfg))

(defmethod ig/init-key :audiophile.repositories/transactor [_ cfg]
  (repos/transactor cfg))

(defmethod ig/init-key :audiophile.repositories/cfg [_ cfg]
  (repos/cfg cfg))

(defmethod ig/init-key :audiophile.repositories/datasource [_ cfg]
  (repos/datasource cfg))

(defmethod ig/init-key :audiophile.repositories/datasource#close [_ cfg]
  (repos/datasource#close cfg))

(defmethod ig/init-key :audiophile.repositories.files/accessor [_ cfg]
  (files/accessor cfg))

(defmethod ig/init-key :audiophile.repositories.files/->executor [_ cfg]
  (qfiles/->executor cfg))

(defmethod ig/init-key :audiophile.repositories.projects/accessor [_ cfg]
  (projects/accessor cfg))

(defmethod ig/init-key :audiophile.repositories.projects/->executor [_ cfg]
  (qprojects/->executor cfg))

(defmethod ig/init-key :audiophile.repositories.teams/accessor [_ cfg]
  (teams/accessor cfg))

(defmethod ig/init-key :audiophile.repositories.teams/->executor [_ cfg]
  (qteams/->executor cfg))

(defmethod ig/init-key :audiophile.repositories.users/accessor [_ cfg]
  (users/accessor cfg))

(defmethod ig/init-key :audiophile.repositories.users/->executor [_ cfg]
  (qusers/->executor cfg))
