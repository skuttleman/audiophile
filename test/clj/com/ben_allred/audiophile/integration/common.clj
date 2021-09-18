(ns com.ben-allred.audiophile.integration.common
  (:require
    [com.ben-allred.audiophile.backend.api.protocols :as papp]
    [com.ben-allred.audiophile.backend.api.repositories.core :as repos]
    [com.ben-allred.audiophile.backend.api.repositories.protocols :as prepos]
    [com.ben-allred.audiophile.backend.infrastructure.system.env :as env]
    [com.ben-allred.audiophile.common.core.utils.logger :as log]
    [com.ben-allred.audiophile.common.infrastructure.duct :as uduct]
    [com.ben-allred.audiophile.common.infrastructure.pubsub.memory :as pubsub.mem]
    [duct.core :as duct]
    [duct.core.env :as env*]
    [integrant.core :as ig]
    [test.utils.stubs :as stubs]
    com.ben-allred.audiophile.backend.dev.handler
    com.ben-allred.audiophile.backend.infrastructure.system.core
    com.ben-allred.audiophile.integration.common.components))

(def ^:private config-base
  (binding [env*/*env* (merge env*/*env* (env/load-env [".env-common" ".env-dev" ".env-test"]))]
    (duct/load-hierarchy)
    (-> "test.edn"
        duct/resource
        (duct/read-config uduct/readers)
        (assoc-in [:duct.profile/base [:duct.custom/merge :routes/table]]
                  #{(ig/ref :routes/table#api) (ig/ref :routes/table#auth)})
        (duct/prep-config [:duct.profile/base :duct.profile/dev :duct.profile/test]))))

(defn ^:private mocked-cfg
  ([base]
   (mocked-cfg base nil))
  ([base opts]
   (let [store (atom {})]
     (-> base
         (assoc [:duct/const :services/oauth]
                (stubs/create (reify
                                papp/IOAuthProvider
                                (redirect-uri [_ _])
                                (profile [_ _]))))
         (assoc [:duct/const :services/s3-client]
                (stubs/create (reify
                                prepos/IKVStore
                                (uri [_ _ _]
                                  "test://uri")
                                (get [_ k _]
                                  (get @store k))
                                (put! [_ k v _]
                                  (swap! store assoc-in [k :Body] v)))))
         (cond->
           (not (:db/enabled? opts))
           (assoc [:duct/const :services/transactor]
                  (stubs/create (reify
                                  prepos/ITransact
                                  (transact! [this f]
                                    (f this))
                                  prepos/IExecute
                                  (execute! [_ _ _]))))

           (not (:rabbitmq/enabled? opts))
           (assoc [:duct/const :services/pubsub]
                  (pubsub.mem/pubsub nil)))))))

(defn setup-stub [config & args]
  (->> args
       (partition 3)
       (reduce (fn [cfg [k m f]]
                 (setup-stub cfg k m f))
               config)))

(defmacro with-config [[sym keys f & f-args] opts & body]
  (let [[opts body] (if (map? opts)
                      [opts body]
                      [nil (cons opts body)])]
    `(let [cfg# (~mocked-cfg ~config-base ~opts)
           system# (-> cfg#
                       ((or ~f identity) ~@f-args)
                       (ig/init ~keys))
           ~sym system#]
       (try
         ~@body
         (finally
           (ig/halt! system#))))))

(defn component [system k]
  (second (ig/find-derived-1 system k)))

(defn ^:private lookup* [system query]
  (let [tx (component system :services/transactor)]
    (first (repos/transact! tx repos/execute! query))))

(defn lookup-user [system email]
  (lookup* system {:select [[:id "user/id"]
                            [:email "user/email"]
                            [:first-name "user/first-name"]
                            [:last-name "user/last-name"]
                            [:handle "user/handle"]]
                   :from   [:users]
                   :where  [:= :email email]}))

(defn lookup-project [system name]
  (lookup* system {:select [[:id "project/id"]
                            [:team-id "project/team-id"]]
                   :from   [:projects]
                   :where  [:= :name name]}))

(defn lookup-artifact [system filename]
  (lookup* system {:select [[:id "artifact/id"]]
                   :from   [:artifacts]
                   :where  [:= :filename filename]}))

(defn lookup-file [system filename]
  (lookup* system {:select [[:id "file/id"]]
                   :from   [:files]
                   :where  [:= :name filename]}))

(defn lookup-file-version [system version-name]
  (lookup* system {:select [[:id "file-version/id"]]
                   :from   [:file-versions]
                   :where  [:= :name version-name]}))

(defn lookup-team [system name]
  (lookup* system {:select [[:id "team/id"]]
                   :from   [:teams]
                   :where  [:= :name name]}))
