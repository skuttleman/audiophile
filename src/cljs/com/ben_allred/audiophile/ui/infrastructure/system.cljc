(ns com.ben-allred.audiophile.ui.infrastructure.system
  #?(:cljs
     (:require-macros
       com.ben-allred.audiophile.ui.infrastructure.system))
  (:require
    #?@(:clj [[com.ben-allred.audiophile.common.infrastructure.duct :as uduct]
              [duct.core :as duct]])
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.core.stubs.dom :as dom]
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

(defmethod ig/init-key :duct.custom/merge [_ ms]
  (reduce merge ms))

(defmethod ig/init-key :audiophile.ui.services/env [_ {:keys [edn]}]
  (some->> dom/window .-ENV (serdes/deserialize edn)))

(defmethod ig/init-key :audiophile.ui.services/base-urls [_ {:keys [env]}]
  {:api  (:api-base env)
   :auth (:auth-base env)})