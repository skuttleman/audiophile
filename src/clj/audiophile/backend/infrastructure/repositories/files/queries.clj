(ns audiophile.backend.infrastructure.repositories.files.queries
  (:require
    [audiophile.backend.infrastructure.repositories.core :as repos]
    [audiophile.backend.infrastructure.db.common :as cdb]
    [audiophile.backend.infrastructure.db.models.core :as models]
    [audiophile.backend.infrastructure.db.models.sql :as sql]
    [audiophile.backend.infrastructure.db.models.tables :as tbl]
    [audiophile.common.core.utils.colls :as colls]
    [audiophile.common.core.utils.logger :as log]))

(defn ^:private has-team-clause [project-id user-id]
  [:and
   [:= :files.project-id project-id]
   [:exists (-> tbl/projects
                (models/select* [:and
                                 [:= :projects.id :files.project-id]
                                 [:= :user-teams.user-id user-id]])
                (models/join tbl/user-teams [:= :user-teams.team-id :projects.team-id])
                (assoc :select [1]))]])

(defn ^:private access* [user-id]
  (-> tbl/projects
      (models/select* [:and [:= :user-teams.user-id user-id]])
      (models/join tbl/user-teams [:= :user-teams.team-id :projects.team-id])))

(defn ^:private access-project [project-id user-id]
  (-> user-id
      access*
      (update :where conj [:= :projects.id project-id])
      (assoc :select [1])))

(defn ^:private access-file [file-id user-id]
  (-> user-id
      access*
      (models/join tbl/files [:= :files.project-id :projects.id])
      (update :where conj [:= :files.id file-id])
      (assoc :select [1])))

(defn ^:private file-access-clause [user-id]
  (-> user-id
      access*
      (update :where conj [:= :projects.id :files.project-id])
      (assoc :select [1])))

(defn ^:private artifact-access-clause [user-id]
  (-> user-id
      access*
      (models/join tbl/files [:= :files.project-id :projects.id])
      (models/join tbl/file-versions [:= :file-versions.file-id :files.id])
      (update :where conj [:= :file-versions.artifact-id :artifacts.id])
      (assoc :select [1])))

(defn ^:private select-by [clause]
  (-> tbl/files
      (models/select-fields #{:id :idx :name :project-id})
      (models/select* clause)
      (models/join (models/model {:table  {:select   [:file-versions.file-id
                                                      [(sql/max :file-versions.selected-at) :selected-at]]
                                           :from     [:file-versions]
                                           :group-by [:file-versions.file-id]}
                                  :fields #{:selected-at}
                                  :alias  :file-version})
                   [:= :file-version.file-id :files.id])
      (models/join (models/model {:table     :file-versions
                                  :namespace :file-version
                                  :alias     :fv})
                   [:and
                    [:= :fv.file-id :file-version.file-id]
                    [:= :fv.selected-at :file-version.selected-at]])
      (update :select into [[:fv.id "file-version/id"]
                            [:fv.name "file-version/name"]
                            [:fv.artifact-id "file-version/artifact-id"]])))

(defn ^:private select-one [file-id]
  (select-by [:= :files.id file-id]))

(defn ^:private select-for-user* [project-id user-id]
  (-> project-id
      (has-team-clause user-id)
      select-by
      (models/order-by [:files.idx :asc]
                       [:file-version.selected-at :desc])))

(defn ^:private select-one-plain [file-id]
  (-> tbl/files
      (models/select-fields #{:id :idx :name :project-id})
      (models/select-by-id* file-id)))

(defn ^:private select-versions [file-id]
  (-> tbl/file-versions
      (models/select-fields #{:id :name :artifact-id :selected-at})
      (models/select* [:= :file-versions.file-id file-id])
      (models/order-by [:file-versions.selected-at :desc])))

(defn ^:private insert [file]
  (models/insert-into tbl/files
                      (assoc file
                             :idx (-> tbl/files
                                      (models/select* [:= :files.project-id (:project-id file)])
                                      (assoc :select [(sql/count :idx)])))))

(defn ^:private insert-artifact [artifact]
  (-> artifact
      (select-keys #{:artifact/content-type :artifact/filename :artifact/key :artifact/uri})
      (assoc :artifact/content-length (:artifact/size artifact))
      (->> (models/insert-into tbl/artifacts))))

(defn ^:private insert-version [version]
  (models/insert-into tbl/file-versions
                      {:artifact-id (:artifact/id version)
                       :file-id     (:file/id version)
                       :name        (:file-version/name version)}))

(defn insert-artifact-access? [_ _ _]
  true)

(defn insert-artifact! [executor artifact _opts]
  (->> artifact
       insert-artifact
       (repos/execute! executor)
       colls/only!
       :id))

(defn find-by-artifact-id [executor artifact-id opts]
  (-> tbl/artifacts
      models/select*
      (assoc :where [:and
                     [:= :artifacts.id artifact-id]
                     [:exists (artifact-access-clause (:user/id opts))]])
      (->> (repos/execute! executor))
      colls/only!))

(defn insert-file-access? [executor {project-id :project/id} {user-id :user/id}]
  (cdb/access? executor (access-project project-id user-id)))

(defn insert-file! [executor file _]
  (-> {:name       (:file/name file)
       :project-id (:project/id file)}
      insert
      (->> (repos/execute! executor))
      colls/only!
      :id))

(defn find-by-file-id [executor file-id opts]
  (when-let [file (-> (if (:includes/versions? opts)
                        (select-one-plain file-id)
                        (select-one file-id))
                      (update :where (fn [clause]
                                       [:and
                                        clause
                                        [:exists
                                         (file-access-clause (:user/id opts))]]))
                      (->> (repos/execute! executor))
                      colls/only!)]
    (cond-> file
      (:includes/versions? opts)
      (assoc :file/versions
             (repos/execute! executor (select-versions file-id))))))

(defn select-for-project [executor project-id opts]
  (repos/execute! executor (select-for-user* project-id (:user/id opts))))

(defn insert-version-access? [executor {file-id :file/id} {user-id :user/id}]
  (cdb/access? executor (access-file file-id user-id)))

(defn insert-version! [executor version _]
  (-> version
      insert-version
      (->> (repos/execute! executor))
      colls/only!
      :id))

(defn select-version-access? [executor {file-id :file/id version-id :file-version/id} {user-id :user/id}]
  (and (cdb/access? executor (access-file file-id user-id))
       (cdb/access? executor (-> tbl/file-versions
                                 models/select*
                                 (assoc :select [1])
                                 (models/and-where [:and
                                                    [:= :file-versions.id version-id]
                                                    [:= :file-versions.file-id file-id]])))))

(defn select-version! [executor {version-id :file-version/id} opts]
  (repos/execute! executor
                  (-> tbl/file-versions
                      models/sql-update
                      (models/sql-set {:selected-at (sql/raw "now()")})
                      (models/and-where [:= :file-versions.id version-id]))
                  opts))

(defn update-file-access? [executor {file-id :file/id} {user-id :user/id}]
  (cdb/access? executor (access-file file-id user-id)))

(defn update-file! [executor {file-id :file/id :file/keys [name]} opts]
  (repos/execute! executor
                  (-> tbl/files
                      models/sql-update
                      (models/sql-set {:name name})
                      (models/and-where [:= :files.id file-id]))
                  opts))

(defn update-version! [executor {version-id :file-version/id :file-version/keys [name]} opts]
  (repos/execute! executor
                  (-> tbl/file-versions
                      models/sql-update
                      (models/sql-set {:name name})
                      (models/and-where [:= :file-versions.id version-id]))
                  opts))
