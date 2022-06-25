(ns audiophile.backend.infrastructure.templates.workflows
  (:require
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.serdes.impl :as serde]
    [clojure.java.io :as io]))

(defn load! [template]
  (let [filename (str "spigot/" (namespace template) "/" (name template) ".edn")]
    (serdes/deserialize serde/edn (io/input-stream (io/resource filename)))))

(defmulti ->ctx identity)
(defmethod ->ctx :default [_] {})

(defmulti ->result identity)
(defmethod ->result :default [_] {})

(defmulti command-handler
          (fn [_executor _sys {:command/keys [type]}]
            type))
