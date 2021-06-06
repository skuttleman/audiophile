(ns com.ben-allred.audiophile.common.domain.validations.core
  (:refer-clojure :exclude [select-keys])
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.domain.validations.specs :as specs]
    [malli.core :as m]
    [malli.error :as me])
  #?(:clj
     (:import
       (clojure.lang IFn))))

(defn ^:private humanize-opts [missing-keys]
  {:errors (assoc me/default-errors
                  ::m/missing-key
                  {:error/fn
                   (fn [{:keys [path]} _]
                     (or (get-in missing-keys path)
                         (str (name (peek path)) " is required")))})})

(def ^:private lookup
  {:api.common/auth       specs/auth
   :api.common/file-id    specs/file-id
   :api.common/project-id specs/project-id
   :api.common/team-id    specs/team-id
   :api.artifact/create   specs/api-artifact:create
   :api.file/create       specs/api-file:create
   :api.project/create    specs/api-project:create
   :api.team/create       specs/api-team:create
   :api.version/create    specs/api-version:create
   :api.ws/connect        specs/api-ws:connect
   :res.version/download  specs/res-version:download})

(defrecord Validator [spec -opts]
  IFn
  (#?(:cljs -invoke :default invoke) [_ input]
    (-> spec
        (m/explain input)
        (me/humanize -opts))))

(defn validator [{:keys [spec]}]
  (->Validator spec (humanize-opts (:missing-keys (meta spec)))))

(defn validate! [spec data]
  (if-let [result (m/explain (lookup spec) data)]
    (do (log/warn "Invalid data" spec (me/humanize result))
        (throw (ex-info "invalid input" {:paths (into #{} (map :path) (:errors result))})))
    data))

(defn select-keys [spec m]
  (->> (m/entries spec)
       (into #{} (map first))
       (clojure.core/select-keys m)))
