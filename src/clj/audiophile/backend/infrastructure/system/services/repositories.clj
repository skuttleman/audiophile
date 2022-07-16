(ns audiophile.backend.infrastructure.system.services.repositories
  (:require
    [audiophile.backend.infrastructure.repositories.comments.impl :as comments]
    [audiophile.backend.infrastructure.repositories.common :as crepos]
    [audiophile.backend.infrastructure.repositories.events.impl :as events]
    [audiophile.backend.infrastructure.repositories.files.impl :as files]
    [audiophile.backend.infrastructure.repositories.projects.impl :as projects]
    [audiophile.backend.infrastructure.repositories.teams.impl :as teams]
    [audiophile.backend.infrastructure.repositories.users.impl :as rusers]
    [audiophile.backend.infrastructure.db.core :as db]
    [audiophile.backend.infrastructure.db.models.core :as models]
    [audiophile.backend.infrastructure.stores :as stores]
    [integrant.core :as ig]))

(defmethod ig/init-key :audiophile.repositories/store [_ cfg]
  (crepos/store cfg))

(defmethod ig/init-key :audiophile.repositories/query-formatter [_ cfg]
  (db/query-formatter cfg))

(defmethod ig/init-key :audiophile.repositories/->builder-fn [_ cfg]
  (models/->builder-fn cfg))

(defmethod ig/init-key :audiophile.repositories/->executor [_ cfg]
  (db/->executor cfg))

(defmethod ig/init-key :audiophile.repositories/transactor [_ cfg]
  (db/transactor cfg))

(defmethod ig/init-key :audiophile.repositories/cfg [_ cfg]
  (db/cfg cfg))

(defmethod ig/init-key :audiophile.repositories/datasource [_ cfg]
  (db/datasource cfg))

(defmethod ig/halt-key! :audiophile.repositories/datasource [_ cfg]
  (db/datasource#close cfg))

(defmethod ig/init-key :audiophile.repositories.comments/accessor [_ cfg]
  (comments/accessor cfg))

(defmethod ig/init-key :audiophile.repositories.events/accessor [_ cfg]
  (events/accessor cfg))

(defmethod ig/init-key :audiophile.repositories.files/accessor [_ cfg]
  (files/accessor cfg))

(defmethod ig/init-key :audiophile.repositories.files/artifact-store [_ cfg]
  (stores/artifact-store cfg))

(defmethod ig/init-key :audiophile.repositories.projects/accessor [_ cfg]
  (projects/accessor cfg))

(defmethod ig/init-key :audiophile.repositories.teams/accessor [_ cfg]
  (teams/accessor cfg))

(defmethod ig/init-key :audiophile.repositories.users/accessor [_ cfg]
  (rusers/accessor cfg))
