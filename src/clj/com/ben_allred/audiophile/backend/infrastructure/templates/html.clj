(ns com.ben-allred.audiophile.backend.infrastructure.templates.html
  (:require
    [com.ben-allred.audiophile.common.core.serdes.core :as serdes]
    [com.ben-allred.audiophile.common.core.serdes.impl :as serde]
    [com.ben-allred.audiophile.common.core.utils.colls :as colls]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [hiccup.core :as hiccup]))

(defn render
  "Renders an html template with user-specific environment variables"
  [template env]
  (let [env (serdes/serialize serde/transit env)]
    (->> template
         (colls/postwalk (fn [form]
                           (let [tag (when (vector? form)
                                       (get form 0))]
                             (cond-> form
                               (= :script#env tag)
                               (conj (str "window.ENV = " (pr-str env) ";"))))))
         (hiccup/html nil)
         (str "<!doctype html>"))))
