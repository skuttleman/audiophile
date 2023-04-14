(ns audiophile.backend.infrastructure.templates.workflows
  (:require
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.serdes.impl :as serde]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.fns :as fns]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [camel-snake-kebab.core :as csk]
    [clojure.java.io :as io]))

(defmulti setup (fn [[tag]]
                  tag))

(defmethod setup :default
  [form]
  [nil form])

(defmethod setup :workflows/setup
  [[_ & more]]
  (let [[opts & children] (cond->> more
                            (not (map? (first more))) (cons {}))]
    [opts (colls/only! children)]))

(defn load! [template]
  (let [filename (str "spigot/"
                      (csk/->snake_case_string (namespace template))
                      "/"
                      (csk/->snake_case_string (name template))
                      ".edn")]
    (serdes/deserialize serde/edn (io/input-stream (io/resource filename)))))

(defn workflow-spec [template ctx]
  (let [[setup form] (setup (load! template))
        [spec params] (maps/extract-keys setup #{:workflows/->result})]
    (assoc spec
           :workflows/template template
           :workflows/form form
           :workflows/params params
           :workflows/ctx (maps/select-rename-keys ctx params))))
