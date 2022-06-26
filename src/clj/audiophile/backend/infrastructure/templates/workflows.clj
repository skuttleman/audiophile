(ns audiophile.backend.infrastructure.templates.workflows
  (:require
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.serdes.impl :as serde]
    [audiophile.common.core.utils.logger :as log]
    [clojure.java.io :as io]))

(defn load! [template]
  (let [filename (str "spigot/" (namespace template) "/" (name template) ".edn")]
    (serdes/deserialize serde/edn (io/input-stream (io/resource filename)))))

(defmulti with-workflow identity)
(defmethod with-workflow :default [_] nil)

(defmulti command-handler
          (fn [_executor _sys {:command/keys [type]}]
            type))

(defmacro defhandler [command-type [ex-bnd sys-bnd msg-bnd :as bnds] & body]
  {:pre [(vector? bnds)
         (= 3 (count bnds))
         (symbol? command-type)]}
  (let [[sys-sym msg-sym ctx-sym] (repeatedly gensym)]
    `(do (require 'audiophile.backend.api.pubsub.core)
         (defmethod command-handler ~(keyword command-type)
           [~ex-bnd {commands# :commands :as ~sys-sym} {~ctx-sym :command/ctx :as ~msg-sym}]
           (log/with-ctx :CP
             (let [~sys-bnd ~sys-sym
                   ~msg-bnd (update ~msg-sym :command/data :spigot/params)
                   body# (do ~@body)
                   result# {:spigot/id     (-> ~msg-sym :command/data :spigot/id)
                            :spigot/result body#}]
               (audiophile.backend.api.pubsub.core/emit-command! commands#
                                                                 :workflow/next!
                                                                 result#
                                                                 ~ctx-sym)
               body#))))))
