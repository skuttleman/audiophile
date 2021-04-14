(ns com.ben-allred.audiophile.ui.services.config
  (:require
    [duct.core :as duct]))

(defmacro load-config [file]
  (duct/load-hierarchy)
  (-> file
      duct/resource
      duct/read-config
      (duct/prep-config [:duct.profile/prod])))
