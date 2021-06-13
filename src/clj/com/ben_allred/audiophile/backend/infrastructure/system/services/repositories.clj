(ns com.ben-allred.audiophile.backend.infrastructure.system.services.repositories
  (:require
    [com.ben-allred.audiophile.backend.api.repositories.common :as crepos]
    [com.ben-allred.audiophile.backend.api.repositories.files.impl :as files]
    [com.ben-allred.audiophile.backend.api.repositories.projects.impl :as projects]
    [com.ben-allred.audiophile.backend.api.repositories.teams.impl :as teams]
    [com.ben-allred.audiophile.backend.api.repositories.users.impl :as rusers]
    [com.ben-allred.audiophile.backend.infrastructure.db.core :as db]
    [com.ben-allred.audiophile.backend.infrastructure.db.events :as db.events]
    [com.ben-allred.audiophile.backend.infrastructure.db.files :as db.files]
    [com.ben-allred.audiophile.backend.infrastructure.db.models.core :as models]
    [com.ben-allred.audiophile.backend.infrastructure.db.projects :as db.projects]
    [com.ben-allred.audiophile.backend.infrastructure.db.teams :as db.teams]
    [com.ben-allred.audiophile.backend.infrastructure.db.users :as db.users]
    [com.ben-allred.audiophile.backend.infrastructure.pubsub.emitter :as emitter]
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
  (models/->builder-fn cfg))

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

(defmethod ig/init-key :audiophile.repositories.events/->executor [_ cfg]
  (db.events/->executor cfg))

(defmethod ig/init-key :audiophile.repositories.events/->emitter [_ cfg]
  (emitter/->emitter cfg))

(defmethod ig/init-key :audiophile.repositories.files/accessor [_ cfg]
  (files/accessor cfg))

(defmethod ig/init-key :audiophile.repositories.files/->file-executor [_ cfg]
  (db.files/->file-executor cfg))

(defmethod ig/init-key :audiophile.repositories.files/->file-event-emitter [_ cfg]
  (db.files/->file-event-emitter cfg))

(defmethod ig/init-key :audiophile.repositories.files/->executor [_ cfg]
  (db.files/->executor cfg))

(defmethod ig/init-key :audiophile.repositories.projects/accessor [_ cfg]
  (projects/accessor cfg))

(defmethod ig/init-key :audiophile.repositories.projects/->project-executor [_ cfg]
  (db.projects/->project-executor cfg))

(defmethod ig/init-key :audiophile.repositories.projects/->project-event-emitter [_ cfg]
  (db.projects/->project-event-emitter cfg))

(defmethod ig/init-key :audiophile.repositories.projects/->executor [_ cfg]
  (db.projects/->executor cfg))

(defmethod ig/init-key :audiophile.repositories.teams/accessor [_ cfg]
  (teams/accessor cfg))

(defmethod ig/init-key :audiophile.repositories.teams/->team-executor [_ cfg]
  (db.teams/->team-executor cfg))

(defmethod ig/init-key :audiophile.repositories.teams/->team-event-emitter [_ cfg]
  (db.teams/->team-event-emitter cfg))

(defmethod ig/init-key :audiophile.repositories.teams/->executor [_ cfg]
  (db.teams/->executor cfg))

(defmethod ig/init-key :audiophile.repositories.users/accessor [_ cfg]
  (rusers/accessor cfg))

(defmethod ig/init-key :audiophile.repositories.users/->executor [_ cfg]
  (db.users/->executor cfg))
