(ns com.ben-allred.audiophile.api.services.repositories.files.queries
  (:require
    [com.ben-allred.audiophile.api.services.repositories.models.core :as models]
    [com.ben-allred.audiophile.api.services.repositories.models.sql :as sql]
    [com.ben-allred.audiophile.common.utils.logger :as log]))

(defn ^:private has-team-clause [project-id user-id]
  [:and
   [:= :files.project-id project-id]
   [:exists {:select [:id]
             :from   [:projects]
             :join   [:user-teams [:= :projects.team-id :user-teams.team-id]]
             :where  [:and
                      [:= :projects.id :files.project-id]
                      [:= :user-teams.user-id user-id]]}]])

(defn ^:private access! [model user-id]
  (-> model
      (models/select-fields #{:id})
      (models/select* [:and
                       [:= :user-teams.user-id user-id]])
      (models/join {:table :user-teams}
                   [:= :user-teams.team-id :projects.team-id])))

(defn access-project! [model project-id user-id]
  (-> model
      (access! user-id)
      (update :where conj [:= :projects.id project-id])))

(defn access-file! [model file-id user-id]
  (-> model
      (access! user-id)
      (models/join {:table :files} [:= :files.project-id :projects.id])
      (update :where conj [:= :files.id file-id])))

(defn access-artifact! [model artifact-id user-id]
  (-> model
      (access! user-id)
      (models/join {:table :files} [:= :files.project-id :projects.id])
      (models/join {:table :file-versions} [:= :file-versions.file-id :files.id])
      (update :where conj [:= :file-versions.artifact-id artifact-id])))

(defn select-by [model clause]
  (-> model
      (models/select-fields #{:id :idx :name :project-id})
      (models/select* clause)
      (models/join {:table  {:select   [:file-versions.file-id
                                        [(sql/max :file-versions.created-at) :created-at]]
                             :from     [:file-versions]
                             :group-by [:file-versions.file-id]}
                    :fields #{:created-at}
                    :alias  :version}
                   [:= :version.file-id :files.id])
      (models/join {:table     :file-versions
                    :namespace :version
                    :fields    #{}
                    :alias     :fv}
                   [:and
                    [:= :fv.file-id :version.file-id]
                    [:= :fv.created-at :version.created-at]])
      (update :select into [[:fv.id "version/id"]
                            [:fv.name "version/name"]
                            [:fv.artifact-id "version/artifact-id"]])))

(defn select-one [model file-id]
  (select-by model [:= :files.id file-id]))

(defn select-for-user [model project-id user-id]
  (-> model
      (select-by (has-team-clause project-id user-id))
      (models/order-by [:files.idx :asc]
                       [:version.created-at :desc])))

(defn select-one-plain [model file-id]
  (-> model
      (models/select-fields #{:id :idx :name :project-id})
      (models/select* [:= :files.id file-id])))

(defn select-versions [model file-id]
  (-> model
      (models/select-fields #{:id :name :artifact-id})
      (models/select* [:= :file-versions.file-id file-id])
      (models/order-by [:file-versions.created-at :desc])))

(defn insert [model file]
  (models/insert-into model
                      (assoc file
                             :idx (-> model
                                      (models/select* [:= :files.project-id (:project-id file)])
                                      (assoc :select [(sql/count :idx)])))))

(defn insert-artifact [model uri artifact user-id]
  (-> artifact
      (select-keys #{:content-type :filename})
      (assoc :uri uri
             :content-length (:size artifact)
             :created-by user-id)
      (->> (models/insert-into model))))

(defn select-artifact [model artifact-id]
  (models/select* model [:= :artifacts.id artifact-id]))

(defn insert-version [model version file-id user-id]
  (models/insert-into model {:artifact-id (:artifact/id version)
                             :file-id     file-id
                             :name        (:version/name version)
                             :created-by  user-id}))
