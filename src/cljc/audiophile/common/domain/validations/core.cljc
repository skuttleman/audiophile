(ns audiophile.common.domain.validations.core
  (:refer-clojure :exclude [select-keys])
  (:require
    [audiophile.common.core.utils.logger :as log]
    [audiophile.common.domain.validations.specs :as specs]
    [clojure.string :as string]
    [malli.core :as m]
    [malli.error :as me]
    [malli.transform :as mt]))

(defn ^:private error-fn [missing-keys]
  (fn [{:keys [path]} _]
    (or (get-in missing-keys path)
        (-> (peek path)
            name
            (string/replace #"-" " ")
            (str " is required")))))

(defn ^:private humanize-opts [missing-keys]
  (let [f (error-fn missing-keys)]
    {:errors (assoc me/default-errors
                    ::me/unknown {:error/fn f}
                    ::m/missing-key {:error/fn f})}))

(def ^:private lookup
  {:api.artifact/create        specs/api-artifact:create
   :api.comment/fetch-all      specs/api-comment:fetch-all
   :api.comment/create         specs/api-comment:create
   :api.common/auth            specs/auth
   :api.common/file-id         specs/file-id
   :api.common/project-id      specs/project-id
   :api.common/team-id         specs/team-id
   :api.events/fetch-all       specs/api-event:fetch-all
   :api.file/create            specs/api-file:create
   :api.file/select-version    specs/api-file:select-version
   :api.file/update            specs/api-file:update
   :api.profile/fetch          specs/profile
   :api.project/create         specs/api-project:create
   :api.project/update         specs/api-project:update
   :api.team/create            specs/api-team:create
   :api.team-invitation/create specs/api-team-invitation:create
   :api.team-invitation/update specs/api-team-invitation:update
   :api.team/update            specs/api-team:update
   :api.user/create            specs/api-user:create
   :api.version/create         specs/api-version:create
   :api.ws/connect             specs/api-ws:connect
   :res.version/download       specs/res-version:download})

(defn validator [{:keys [spec]}]
  (let [opts (humanize-opts (:missing-keys (meta spec)))]
    (fn [input]
      (-> spec
          (m/explain input)
          (me/humanize opts)))))

(defn validate! [spec data]
  (if-let [result (m/explain (lookup spec spec) data)]
    (throw (ex-info "invalid input" {:paths   (into #{} (map :path) (:errors result))
                                     :details (me/humanize result)}))
    data))

(defn select-keys [spec m]
  (->> (m/entries spec)
       (into #{} (map first))
       (clojure.core/select-keys m)))

(defn conform [spec value]
  (try (m/decode spec value mt/string-transformer)
       (catch #?(:cljs :default :default Throwable) _
         value)))
