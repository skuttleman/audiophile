(ns com.ben-allred.audiophile.integration.common
  (:require
    [com.ben-allred.audiophile.api.services.auth.protocols :as pauth]
    [com.ben-allred.audiophile.api.services.env :as env]
    [com.ben-allred.audiophile.api.services.repositories.protocols :as prepos]
    [com.ben-allred.audiophile.common.utils.duct :as uduct]
    [duct.core :as duct]
    [duct.core.env :as env*]
    [integrant.core :as ig]
    [test.utils.stubs :as stubs]))

(def ^:private config-base
  (binding [env*/*env* (merge env*/*env* (env/load-env [".env" ".env-dev" ".env-test"]))]
    (duct/load-hierarchy)
    (-> "test.edn"
        duct/resource
        (duct/read-config uduct/readers)
        (duct/prep-config [:duct.profile/base :duct.profile/test]))))

(defn ^:private mocked-cfg
  ([base]
   (mocked-cfg base nil))
  ([base opts]
   (-> base
       (assoc [:duct/const :services/oauth]
              (stubs/create (reify
                              pauth/IOAuthProvider
                              (-redirect-uri [_ _])
                              (-profile [_ _]))))
       (cond->
         (not (:db/enabled? opts))
         (assoc [:duct/const :services/transactor]
                (stubs/create (reify
                                prepos/ITransact
                                (transact! [this f] (f this nil))
                                prepos/IExecute
                                (execute! [_ _ _]))))))))

(defn setup-stub [config stub-k method f-or-val]
  (update config [:duct/const stub-k] stubs/set-stub! method f-or-val))

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

(defn seed-user [system email]
  (->> (get-in (get system [:duct/const :test/seed-data]) [0 :values])
       (filter (comp #{email} :email))
       first
       (into {}
             (map (fn [[k v]]
                    [(keyword "user" (name k)) v])))))

(defn component [system k]
  (second (ig/find-derived-1 system k)))
