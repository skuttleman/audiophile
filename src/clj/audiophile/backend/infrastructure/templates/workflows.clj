(ns audiophile.backend.infrastructure.templates.workflows
  (:require
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.serdes.impl :as serde]
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.core.utils.maps :as maps]
    [clojure.java.io :as io]))

(defn load! [template]
  (let [filename (str "spigot/" (namespace template) "/" (name template) ".edn")]
    (serdes/deserialize serde/edn (io/input-stream (io/resource filename)))))

(defn setup [[tag & more :as form]]
  (let [[opts & children] (cond->> more
                            (not (map? (first more))) (cons {}))]
    (if (= tag :workflows/setup)
      (do (assert (= 1 (count children)) "workflows/setup supports exactly 1 child")
          [opts (first children)])
      [nil form])))

(defn workflow-spec [template ctx]
  (let [[setup form] (setup (load! template))
        [spec context] (maps/extract-keys setup #{:workflows/->result})]
    (assoc spec
           :workflows/template template
           :workflows/form form
           :workflows/ctx (maps/select-rename-keys ctx context))))
