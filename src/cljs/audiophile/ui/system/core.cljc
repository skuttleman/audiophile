(ns audiophile.ui.system.core
  #?(:cljs
     (:require-macros
       audiophile.ui.system.core))
  (:require
    #?@(:clj  [[audiophile.common.infrastructure.duct :as uduct]
               [duct.core :as duct]]
        :cljs [[audiophile.ui.utils.env :as env]
               [audiophile.ui.pages.login :as login]
               audiophile.ui.system.components])
    [audiophile.common.core.utils.logger :as log]
    [integrant.core :as ig]
    audiophile.common.infrastructure.system.core))

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
