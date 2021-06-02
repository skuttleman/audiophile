(ns com.ben-allred.audiophile.common.infrastructure.system.services.resources
  (:require
    [com.ben-allred.audiophile.common.app.resources.base :as bres]
    [com.ben-allred.audiophile.common.app.resources.cached :as cached]
    [com.ben-allred.audiophile.common.app.resources.multi :as multi]
    [com.ben-allred.audiophile.common.app.resources.redirect :as redirect]
    [com.ben-allred.audiophile.common.app.resources.toaster :as toaster]
    [com.ben-allred.audiophile.common.app.resources.users :as rusers]
    [com.ben-allred.audiophile.common.app.resources.validated :as vres]
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

(defmethod ig/init-key :audiophile.resources/redirect [_ cfg]
  (redirect/resource cfg))

(defmethod ig/init-key :audiophile.resources/toaster [_ cfg]
  (toaster/resource cfg))

(defmethod ig/init-key :audiophile.resources.toaster/result-fn [_ cfg]
  (toaster/toast-fn cfg))

(defmethod ig/init-key :audiophile.resources.user/login-fn [_ cfg]
  (rusers/login-fn cfg))

(defmethod ig/init-key :audiophile.resource.validated/opts->request [_ cfg]
  (vres/opts->request cfg))