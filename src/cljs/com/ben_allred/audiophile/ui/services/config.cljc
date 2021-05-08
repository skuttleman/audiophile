(ns com.ben-allred.audiophile.ui.services.config
  #?(:cljs
     (:require-macros
       com.ben-allred.audiophile.ui.services.config))
  (:require
    #?(:clj [duct.core :as duct])
    [com.ben-allred.audiophile.common.utils.duct :as uduct]
    [integrant.core :as ig]))

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
