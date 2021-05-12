(ns com.ben-allred.audiophile.api.services.repositories.files.queries
  (:require
    [com.ben-allred.audiophile.api.services.repositories.entities.core :as entities]
    [com.ben-allred.audiophile.api.services.repositories.entities.sql :as sql]))

(defn ^:private has-team-clause [project-id user-id]
  [:and
   [:= :files.project-id project-id]
   [:exists {:select [:id]
             :from   [:projects]
             :join   [:user-teams [:= :projects.team-id :user-teams.team-id]]
             :where  [:and
                      [:= :projects.id :files.project-id]
                      [:= :user-teams.user-id user-id]]}]])


(defn access! [entity user-id]
  (-> entity
      (entities/select-fields #{:id})
      (entities/select* [:and
                         [:= :user-teams.user-id user-id]])
      (entities/join {:table :user-teams}
                     [:= :user-teams.team-id :projects.team-id])))

(defn with-file-access [query file-id]
  (-> query
      (entities/join {:table :files} [:= :files.project-id :projects.id])
      (update :where conj [:= :files.id file-id])))

(defn select-by [entity clause]
  (-> entity
      (entities/select-fields #{:id :idx :name :project-id})
      (entities/select* clause)
      (entities/join {:table  {:select   [:file-versions.file-id
                                          [(sql/max :file-versions.created-at) :created-at]]
                               :from     [:file-versions]
                               :group-by [:file-versions.file-id]}
                      :fields #{:created-at}
                      :alias  :version}
                     [:= :version.file-id :files.id])
      (entities/join {:table     :file-versions
                      :namespace :version
                      :fields    #{}
                      :alias     :fv}
                     [:and
                      [:= :fv.file-id :version.file-id]
                      [:= :fv.created-at :version.created-at]])
      (update :select into [[:fv.id "version/id"]
                            [:fv.name "version/name"]
                            [:fv.artifact-id "version/artifact-id"]])))

(defn select-one [entity file-id]
  (select-by entity [:= :files.id file-id]))

(defn select-for-user [entity project-id user-id]
  (-> entity
      (select-by (has-team-clause project-id user-id))
      (entities/order-by [:files.idx :asc]
                         [:version.created-at :desc])))

(defn insert [entity file]
  (entities/insert-into entity
                        (assoc file
                               :idx (sql/coalesce (-> entity
                                                      (select-by [:= :files.project-id (:project-id file)])
                                                      (assoc :select [(sql/max :idx)]))
                                                  0))))

(defn insert-artifact [entity uri artifact user-id]
  (-> artifact
      (select-keys #{:content-type :filename})
      (assoc :uri uri
             :content-length (:size artifact)
             :created-by user-id)
      (->> (entities/insert-into entity))))

(defn insert-version [entity version file-id user-id]
  (entities/insert-into entity {:artifact-id (:artifact/id version)
                                :file-id     file-id
                                :name        (:version/name version)
                                :created-by  user-id}))
