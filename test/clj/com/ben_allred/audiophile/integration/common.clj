(ns com.ben-allred.audiophile.integration.common
  (:require
    [com.ben-allred.audiophile.api.services.auth.protocols :as pauth]
    [com.ben-allred.audiophile.api.services.env :as env]
    [com.ben-allred.audiophile.api.services.repositories.protocols :as prepos]
    [com.ben-allred.audiophile.common.utils.duct :as uduct]
    [com.ben-allred.audiophile.integration.common.mocks :as mocks]
    [duct.core :as duct]
    [duct.core.env :as env*]
    [integrant.core :as ig]))

(def ^:private config-base
  (binding [env*/*env* (merge env*/*env* (env/load-env [".env" ".env-dev" ".env-test"]))]
    (duct/load-hierarchy)
    (-> "test.edn"
        duct/resource
        (duct/read-config uduct/readers)
        (duct/prep-config [:duct.profile/base :duct.profile/test]))))

(defn ^:private mocked-cfg [base]
  (assoc base
         [:duct/const :services/transactor]
         (mocks/->mock (reify
                         prepos/ITransact
                         (transact! [this f] (f this))
                         prepos/IExecute
                         (execute! [_ _ _])))
         [:duct/const :services/oauth]
         (mocks/->mock (reify
                         pauth/IOAuthProvider
                         (-redirect-uri [_ _])
                         (-profile [_ _])))))

(defn setup-mock
  ([config mock-k method arity f-or-val]
   (setup-mock config mock-k [method arity] f-or-val))
  ([config mock-k method f-or-val]
   (update config [:duct/const mock-k] mocks/set-mock! method f-or-val)))

(defn setup-mocks [config & args]
  (->> args
       (partition 3)
       (reduce (fn [cfg [k m f]]
                 (setup-mock cfg k m f))
               config)))

(defmacro with-config [[sym keys f & f-args] & body]
  `(let [cfg# (~mocked-cfg ~config-base)
         system# (-> cfg#
                     ((or ~f identity) ~@f-args)
                     (ig/init ~keys))
         ~sym system#]
     (try
       ~@body
       (finally
         (ig/halt! system#)))))
