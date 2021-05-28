(ns com.ben-allred.audiophile.common.services.config.views
  (:require
    [com.ben-allred.audiophile.common.views.components.audio :as audio]
    [com.ben-allred.audiophile.common.views.components.modal :as modal]
    [com.ben-allred.audiophile.common.views.components.tiles :as tiles]
    [com.ben-allred.audiophile.common.views.components.toast :as toast]
    [com.ben-allred.audiophile.common.views.core :as views]
    [com.ben-allred.audiophile.common.views.roots.files :as views.files]
    [com.ben-allred.audiophile.common.views.roots.home :as views.home]
    [com.ben-allred.audiophile.common.views.roots.login :as views.login]
    [com.ben-allred.audiophile.common.views.roots.projects :as views.projects]
    [com.ben-allred.audiophile.common.views.roots.teams :as views.teams]
    [integrant.core :as ig]))

(defmethod ig/init-key :audiophile.ui.views/app [_ cfg]
  (views/root cfg))

(defmethod ig/init-key :audiophile.ui.views.files/one [_ cfg]
  (views.files/one cfg))

(defmethod ig/init-key :audiophile.ui.views.home/header [_ cfg]
  (views.home/header cfg))

(defmethod ig/init-key :audiophile.ui.views.home/root [_ cfg]
  (views.home/root cfg))

(defmethod ig/init-key :audiophile.ui.views.login/form [_ cfg]
  (views.login/form cfg))

(defmethod ig/init-key :audiophile.ui.views.login/root [_ cfg]
  (views.login/root cfg))

(defmethod ig/init-key :audiophile.ui.views.projects/version-form [_ cfg]
  (views.projects/version-form cfg))

(defmethod ig/init-key :audiophile.ui.views.projects/file-form [_ cfg]
  (views.projects/file-form cfg))

(defmethod ig/init-key :audiophile.ui.views.projects/track-list [_ cfg]
  (views.projects/track-list cfg))

(defmethod ig/init-key :audiophile.ui.views.projects/one [_ cfg]
  (views.projects/one cfg))

(defmethod ig/init-key :audiophile.ui.views.projects/list [_ cfg]
  (views.projects/list cfg))

(defmethod ig/init-key :audiophile.ui.views.projects/create [_ cfg]
  (views.projects/create cfg))

(defmethod ig/init-key :audiophile.ui.views.teams/list [_ cfg]
  (views.teams/list cfg))

(defmethod ig/init-key :audiophile.ui.views.teams/create [_ cfg]
  (views.teams/create cfg))

(defmethod ig/init-key :audiophile.ui.views.components/audio-player [_ cfg]
  (audio/player cfg))

(defmethod ig/init-key :audiophile.ui.views.components/audio-resource [_ cfg]
  (audio/resource cfg))

(defmethod ig/init-key :audiophile.ui.views.components/modals [_ cfg]
  (modal/modals cfg))

(defmethod ig/init-key :audiophile.ui.views.components/banners [_ cfg]
  (toast/banners cfg))

(defmethod ig/init-key :audiophile.ui.views.components/tile [_ cfg]
  (tiles/with-resource cfg))

(defmethod ig/init-key :audiophile.ui.views.components/toasts [_ cfg]
  (toast/toasts cfg))
