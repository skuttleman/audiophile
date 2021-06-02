(ns com.ben-allred.audiophile.common.infrastructure.system.views
  (:require
    [com.ben-allred.audiophile.common.core.ui-components.audio :as audio]
    [com.ben-allred.audiophile.common.core.ui-components.modal :as modal]
    [com.ben-allred.audiophile.common.core.ui-components.tiles :as tiles]
    [com.ben-allred.audiophile.common.core.ui-components.toast :as toast]
    [com.ben-allred.audiophile.common.infrastructure.views.core :as views]
    [com.ben-allred.audiophile.common.infrastructure.views.roots.files :as views.files]
    [com.ben-allred.audiophile.common.infrastructure.views.roots.home :as views.home]
    [com.ben-allred.audiophile.common.infrastructure.views.roots.login :as views.login]
    [com.ben-allred.audiophile.common.infrastructure.views.roots.projects :as views.projects]
    [com.ben-allred.audiophile.common.infrastructure.views.roots.teams :as views.teams]
    [integrant.core :as ig]))

(defmethod ig/init-key :audiophile.views/app [_ cfg]
  (views/root cfg))

(defmethod ig/init-key :audiophile.views.files/one [_ cfg]
  (views.files/one cfg))

(defmethod ig/init-key :audiophile.views.home/header [_ cfg]
  (views.home/header cfg))

(defmethod ig/init-key :audiophile.views.home/root [_ cfg]
  (views.home/root cfg))

(defmethod ig/init-key :audiophile.views.login/form [_ cfg]
  (views.login/form cfg))

(defmethod ig/init-key :audiophile.views.login/root [_ cfg]
  (views.login/root cfg))

(defmethod ig/init-key :audiophile.views.projects/version-form [_ cfg]
  (views.projects/version-form cfg))

(defmethod ig/init-key :audiophile.views.projects/file-form [_ cfg]
  (views.projects/file-form cfg))

(defmethod ig/init-key :audiophile.views.projects/track-list [_ cfg]
  (views.projects/track-list cfg))

(defmethod ig/init-key :audiophile.views.projects/one [_ cfg]
  (views.projects/one cfg))

(defmethod ig/init-key :audiophile.views.projects/list [_ cfg]
  (views.projects/list cfg))

(defmethod ig/init-key :audiophile.views.projects/create [_ cfg]
  (views.projects/create cfg))

(defmethod ig/init-key :audiophile.views.teams/list [_ cfg]
  (views.teams/list cfg))

(defmethod ig/init-key :audiophile.views.teams/create [_ cfg]
  (views.teams/create cfg))

(defmethod ig/init-key :audiophile.views.components/audio-player [_ cfg]
  (audio/player cfg))

(defmethod ig/init-key :audiophile.views.components/audio-resource [_ cfg]
  (audio/resource cfg))

(defmethod ig/init-key :audiophile.views.components/modals [_ cfg]
  (modal/modals cfg))

(defmethod ig/init-key :audiophile.views.components/banners [_ cfg]
  (toast/banners cfg))

(defmethod ig/init-key :audiophile.views.components/tile [_ cfg]
  (tiles/with-resource cfg))

(defmethod ig/init-key :audiophile.views.components/toasts [_ cfg]
  (toast/toasts cfg))
