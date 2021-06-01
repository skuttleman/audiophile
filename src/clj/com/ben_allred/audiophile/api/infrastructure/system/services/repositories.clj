(ns com.ben-allred.audiophile.api.infrastructure.system.services.repositories
  (:require
    [com.ben-allred.audiophile.api.infrastructure.db.core :as db]
    [com.ben-allred.audiophile.api.infrastructure.db.files :as db.files]
    [com.ben-allred.audiophile.api.infrastructure.db.models.core :as models]
    [com.ben-allred.audiophile.api.infrastructure.db.projects :as db.projects]
    [com.ben-allred.audiophile.api.infrastructure.db.teams :as db.teams]
    [com.ben-allred.audiophile.api.infrastructure.db.users :as db.users]
    [com.ben-allred.audiophile.api.app.repositories.common :as crepos]
    [com.ben-allred.audiophile.api.app.repositories.files.core :as files]
    [com.ben-allred.audiophile.api.app.repositories.projects.core :as projects]
    [com.ben-allred.audiophile.api.app.repositories.teams.core :as teams]
    [com.ben-allred.audiophile.api.app.repositories.users.core :as users]
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
  (db/query-formatter cfg))

(defmethod ig/init-key :audiophile.repositories/raw-formatter [_ cfg]
  (db/raw-formatter cfg))

(defmethod ig/init-key :audiophile.repositories/->builder-fn [_ cfg]
  (db/->builder-fn cfg))

(defmethod ig/init-key :audiophile.repositories/->executor [_ cfg]
  (db/->executor cfg))

(defmethod ig/init-key :audiophile.repositories/transactor [_ cfg]
  (db/transactor cfg))

(defmethod ig/init-key :audiophile.repositories/cfg [_ cfg]
  (db/cfg cfg))

(defmethod ig/init-key :audiophile.repositories/datasource [_ cfg]
  (db/datasource cfg))

(defmethod ig/init-key :audiophile.repositories/datasource#close [_ cfg]
  (db/datasource#close cfg))

(defmethod ig/init-key :audiophile.repositories.files/accessor [_ cfg]
  (files/accessor cfg))

(defmethod ig/init-key :audiophile.repositories.files/->executor [_ cfg]
  (db.files/->executor cfg))

(defmethod ig/init-key :audiophile.repositories.projects/accessor [_ cfg]
  (projects/accessor cfg))

(defmethod ig/init-key :audiophile.repositories.projects/->executor [_ cfg]
  (db.projects/->executor cfg))

(defmethod ig/init-key :audiophile.repositories.teams/accessor [_ cfg]
  (teams/accessor cfg))

(defmethod ig/init-key :audiophile.repositories.teams/->executor [_ cfg]
  (db.teams/->executor cfg))

(defmethod ig/init-key :audiophile.repositories.users/accessor [_ cfg]
  (users/accessor cfg))

(defmethod ig/init-key :audiophile.repositories.users/->executor [_ cfg]
  (db.users/->executor cfg))
