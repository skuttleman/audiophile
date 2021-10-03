(ns com.ben-allred.audiophile.ui.infrastructure.system.services.resources
  (:require
    [com.ben-allred.audiophile.ui.api.components.player :as player]
    [com.ben-allred.audiophile.ui.api.forms.submittable :as form.sub]
    [com.ben-allred.audiophile.ui.infrastructure.resources.base :as bres]
    [com.ben-allred.audiophile.ui.infrastructure.resources.cached :as cached]
    [com.ben-allred.audiophile.ui.infrastructure.resources.custom :as cres]
    [com.ben-allred.audiophile.ui.infrastructure.resources.multi :as multi]
    [com.ben-allred.audiophile.ui.infrastructure.resources.progress :as prog]
    [com.ben-allred.audiophile.ui.infrastructure.resources.redirect :as redirect]
    [com.ben-allred.audiophile.ui.infrastructure.resources.toaster :as toaster]
    [com.ben-allred.audiophile.ui.infrastructure.resources.users :as rusers]
    [integrant.core :as ig]))

(defmethod ig/init-key :audiophile.resources/cached [_ cfg]
  (cached/resource cfg))

(defmethod ig/init-key :audiophile.resources/base [_ cfg]
  (bres/base cfg))

(defmethod ig/init-key :audiophile.resources/http-handler [_ cfg]
  (bres/http-handler cfg))

(defmethod ig/init-key :audiophile.resources/file-uploader [_ cfg]
  (bres/file-uploader cfg))

(defmethod ig/init-key :audiophile.resources/multi [_ cfg]
  (multi/resource cfg))

(defmethod ig/init-key :audiophile.resources/progress [_ cfg]
  (prog/resource cfg))

(defmethod ig/init-key :audiophile.resources/redirect [_ cfg]
  (redirect/resource cfg))

(defmethod ig/init-key :audiophile.resources/toaster [_ cfg]
  (toaster/resource cfg))

(defmethod ig/init-key :audiophile.resources.custom/comment-fetcher [_ cfg]
  (cres/comments-fetcher cfg))

(defmethod ig/init-key :audiophile.resources.custom/comment-poster [_ cfg]
  (cres/comment-poster cfg))

(defmethod ig/init-key :audiophile.resources.toaster/result-fn [_ cfg]
  (toaster/toast-fn cfg))

(defmethod ig/init-key :audiophile.resources.user/login-fn [_ cfg]
  (rusers/login-fn cfg))

(defmethod ig/init-key :audiophile.resource.validated/opts->request [_ cfg]
  (form.sub/opts->request cfg))

(defmethod ig/init-key :audiophile.resource.audio/artifact [_ cfg]
  (cres/res-artifact cfg))

(defmethod ig/init-key :audiophile.views.components/artifact-player [_ cfg]
  (player/artifact-player cfg))
