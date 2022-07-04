(ns audiophile.backend.infrastructure.repositories.workflows.queries
  (:require
    [audiophile.backend.infrastructure.db.models.core :as models]
    [audiophile.backend.infrastructure.db.models.tables :as tbl]
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.common.core.serdes.core :as serdes]
    [audiophile.common.core.serdes.impl :as serde]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.maps :as maps]))

(defn update-by-id! [executor id workflow]
  (repos/execute! executor
                  (-> tbl/workflows
                      models/sql-update
                      (models/sql-set (maps/update-maybe workflow :data (partial serdes/serialize serde/edn)))
                      (models/and-where [:= :workflows.id id]))))

(defn create! [executor data]
  (-> executor
      (repos/execute! (models/insert-into tbl/workflows
                                          {:data (serdes/serialize serde/edn data)}))
      colls/only!
      :id))
