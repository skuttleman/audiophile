(ns audiophile.backend.infrastructure.system.handlers
  (:require
    [audiophile.backend.api.handlers.auth :as auth]
    [audiophile.backend.infrastructure.http.core :as handlers]
    [audiophile.backend.api.handlers.comments :as comments]
    [audiophile.backend.api.handlers.events :as events]
    [audiophile.backend.api.handlers.files :as files]
    [audiophile.backend.api.handlers.projects :as projects]
    [audiophile.backend.api.handlers.search :as search]
    [audiophile.backend.api.handlers.teams :as teams]
    [audiophile.backend.api.handlers.users :as users]
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

(defmethod ig/init-key :audiophile.handlers.comments/fetch-all [_ cfg]
  (comments/fetch-all cfg))

(defmethod ig/init-key :audiophile.handlers.comments/create [_ cfg]
  (comments/create cfg))

(defmethod ig/init-key :audiophile.handlers.events/fetch-all [_ cfg]
  (events/fetch-all cfg))

(defmethod ig/init-key :audiophile.handlers.files/upload [_ cfg]
  (files/upload cfg))

(defmethod ig/init-key :audiophile.handlers.files/fetch-all [_ cfg]
  (files/fetch-all cfg))

(defmethod ig/init-key :audiophile.handlers/search [_ cfg]
  (search/search cfg))

(defmethod ig/init-key :audiophile.handlers.files/fetch [_ cfg]
  (files/fetch cfg))

(defmethod ig/init-key :audiophile.handlers.files/create [_ cfg]
  (files/create cfg))

(defmethod ig/init-key :audiophile.handlers.files/create-version [_ cfg]
  (files/create-version cfg))

(defmethod ig/init-key :audiophile.handlers.files/download [_ cfg]
  (files/download cfg))

(defmethod ig/init-key :audiophile.handlers.users/profile [_ cfg]
  (users/profile cfg))

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

(defmethod ig/init-key :audiophile.handlers.users/create [_ cfg]
  (users/create cfg))
