(ns com.ben-allred.audiophile.ui.infrastructure.system.core
  #?(:cljs
     (:require-macros
       com.ben-allred.audiophile.ui.infrastructure.system.core))
  (:require
    #?@(:clj  [[com.ben-allred.audiophile.common.infrastructure.duct :as uduct]
               [duct.core :as duct]]
        :cljs [[com.ben-allred.audiophile.ui.infrastructure.env :as env]])
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [integrant.core :as ig]
    com.ben-allred.audiophile.common.infrastructure.system.core))

(defmacro load-config [file profiles]
  (duct/load-hierarchy)
  (-> file
      duct/resource
      (duct/read-config uduct/readers)
      (duct/prep-config profiles)))

(defmethod ig/init-key :duct/const [_ component]
  component)

(defmethod ig/init-key :duct.core/project-ns [_ ns]
  ns)

(defmethod ig/init-key :duct.core/environment [_ env]
  env)

#?(:cljs
   (defmethod ig/init-key :audiophile.ui.services/base-urls [_]
     (select-keys env/env #{:api-base :auth-base})))
