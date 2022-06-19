(ns audiophile.test.utils.repositories
  (:require
    [audiophile.backend.infrastructure.repositories.protocols :as prepos]
    [audiophile.test.utils.stubs :as stubs]))

(defn stub-kv-store []
  (stubs/create (reify
                  prepos/IKVStore
                  (uri [_ key _]
                    (str "test://uri/" key))
                  (get [_ _ _])
                  (put! [_ _ _ _]))))

(defn stub-transactor []
  (stubs/create (reify
                  prepos/ITransact
                  (transact! [this f]
                    (f this))

                  prepos/IExecute
                  (execute! [_ _ _]))))
