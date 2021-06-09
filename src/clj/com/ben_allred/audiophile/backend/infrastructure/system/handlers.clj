(ns com.ben-allred.audiophile.backend.infrastructure.system.handlers
  (:require
    [com.ben-allred.audiophile.backend.api.handlers.auth :as auth]
    [com.ben-allred.audiophile.backend.infrastructure.http.core :as handlers]
    [com.ben-allred.audiophile.backend.api.handlers.files :as files]
    [com.ben-allred.audiophile.backend.api.handlers.projects :as projects]
    [com.ben-allred.audiophile.backend.api.handlers.teams :as teams]
    [integrant.core :as ig]))

(defmethod ig/init-key :audiophile.handlers/router [_ cfg]
  (handlers/router cfg))

(defmethod ig/init-key :audiophile.handlers/app [_ cfg]
  (handlers/app cfg))

(defmethod ig/init-key :audiophile.handlers.auth/login [_ cfg]
  (auth/login cfg))

(defmethod ig/init-key :audiophile.handlers.auth/logout [_ cfg]
  (auth/logout cfg))

(defmethod ig/init-key :audiophile.handlers.auth/callback-url [_ cfg]
  (auth/callback-url cfg))

(defmethod ig/init-key :audiophile.handlers.auth/callback [_ cfg]
  (auth/callback cfg))

(defmethod ig/init-key :audiophile.handlers.files/upload [_ cfg]
  (files/upload cfg))

(defmethod ig/init-key :audiophile.handlers.files/fetch-all [_ cfg]
  (files/fetch-all cfg))

(defmethod ig/init-key :audiophile.handlers.files/fetch [_ cfg]
  (files/fetch cfg))

(defmethod ig/init-key :audiophile.handlers.files/create [_ cfg]
  (files/create cfg))

(defmethod ig/init-key :audiophile.handlers.files/create-version [_ cfg]
  (files/create-version cfg))

(defmethod ig/init-key :audiophile.handlers.files/download [_ cfg]
  (files/download cfg))

(defmethod ig/init-key :audiophile.handlers.projects/fetch-all [_ cfg]
  (projects/fetch-all cfg))

(defmethod ig/init-key :audiophile.handlers.projects/fetch [_ cfg]
  (projects/fetch cfg))

(defmethod ig/init-key :audiophile.handlers.projects/create [_ cfg]
  (projects/create cfg))

(defmethod ig/init-key :audiophile.handlers.teams/fetch-all [_ cfg]
  (teams/fetch-all cfg))

(defmethod ig/init-key :audiophile.handlers.teams/fetch [_ cfg]
  (teams/fetch cfg))

(defmethod ig/init-key :audiophile.handlers.teams/create [_ cfg]
  (teams/create cfg))
