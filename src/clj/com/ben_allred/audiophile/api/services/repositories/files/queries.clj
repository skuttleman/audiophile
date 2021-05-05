(ns com.ben-allred.audiophile.api.services.repositories.files.queries
  (:require
    [com.ben-allred.audiophile.api.services.repositories.entities.core :as entities]
    [com.ben-allred.audiophile.api.services.repositories.entities.sql :as sql]))

(defn ^:private has-team-clause [project-id user-id]
  [:exists {:select [:id]
            :from   [:projects]
            :join   [:user-teams [:= :projects.team-id :user-teams.team-id]]
            :where  [:and [:= :projects.id project-id]
                     [:= :user-teams.user-id user-id]]}])

(defn select-by [entity clause]
  (-> entity
      (entities/select-fields #{:id :name :idx})
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
                            [:fv.artifact-id "version/artifact-id"]])
      (entities/order-by [:files.idx :asc]
                         [:version.created-at :desc])))

(defn select-one [entity file-id]
  (select-by entity [:= :files.id file-id]))

(defn select-for-user [entity project-id user-id]
  (select-by entity (has-team-clause project-id user-id)))

(defn insert [entity file]
  (entities/insert-into entity (assoc file :idx (-> entity
                                                    (select-by [:= :files.project-id (:project-id file)])
                                                    (assoc :select [(sql/max :idx)])))))
