(ns com.ben-allred.audiophile.ui.services.config
  #?(:cljs
     (:require-macros
       com.ben-allred.audiophile.ui.services.config))
  (:require
    [com.ben-allred.audiophile.common.utils.duct :as uduct]
    #?(:clj [duct.core :as duct])))

(defmacro load-config [file profiles]
  (duct/load-hierarchy)
  (-> file
      duct/resource
      (duct/read-config uduct/readers)
      (duct/prep-config profiles)))
