(ns com.ben-allred.audiophile.common.domain.validations.core
  (:refer-clojure :exclude [select-keys])
  (:require
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.domain.validations.specs :as specs]
    [malli.core :as m]
    [malli.error :as me]
    [malli.transform :as mt])
  #?(:clj
     (:import
       (clojure.lang IFn))))

(defn ^:private type->spec [type]
  (case type
    :user-defined keyword?
    (:text :character-varying) string?
    :integer integer?
    :jsonb any?
    :timestamp-without-time-zone inst?
    :uuid uuid?
    :numrange (every-pred vector?
                          (comp number? first)
                          (comp number? second))))

(defn ^:private error-fn [missing-keys]
  (fn [{:keys [path]} _]
    (or (get-in missing-keys path)
        (str (name (peek path)) " is required"))))

(defn ^:private humanize-opts [missing-keys]
  (let [f (error-fn missing-keys)]
    {:errors (assoc me/default-errors
                    ::me/unknown {:error/fn f}
                    ::m/missing-key {:error/fn f})}))

(defn ^:private field->spec [table column type nilable?]
  (let [spec (type->spec type)]
    (-> [(keyword table column)]
        (cond-> nilable? (conj {:optional true}))
        (conj spec))))

(def ^:private lookup
  {:api.common/auth       specs/auth
   :api.common/file-id    specs/file-id
   :api.common/project-id specs/project-id
   :api.common/search     specs/search
   :api.common/team-id    specs/team-id
   :api.artifact/create   specs/api-artifact:create
   :api.comment/create    specs/api-comment:create
   :api.events/fetch-all  specs/api-event:fetch-all
   :api.file/create       specs/api-file:create
   :api.project/create    specs/api-project:create
   :api.team/create       specs/api-team:create
   :api.user/create       specs/api-user:create
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
    (throw (ex-info "invalid input" {:paths   (into #{} (map :path) (:errors result))
                                     :details (me/humanize result)}))
    data))

(defn select-keys [spec m]
  (->> (m/entries spec)
       (into #{} (map first))
       (clojure.core/select-keys m)))

(defn ->model-spec [m namespace]
  (let [ns (name namespace)]
    (reduce (fn [result [column [type nilable?]]]
              (conj result (field->spec ns (name column) type nilable?)))
            [:map]
            m)))

(defn conform [spec value]
  (try (m/decode spec value mt/string-transformer)
       (catch #?(:cljs :default :default Throwable) _
         value)))
